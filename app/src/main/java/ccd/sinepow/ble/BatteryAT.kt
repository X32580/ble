package ccd.sinepow.ble

import java.lang.Exception
import java.math.BigDecimal
import java.util.*

/**
 * 作者 ： yp
 * 时间 ： 2020/9/1
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ：
 * 更新 ：
 */
object BatteryAT {

    //命令 版本
    const val OLD_VERSION = "1.0.0"

    const val UNLOCK = "password-" //开锁指令

    const val TIME_ = "time-" //时间指令

    const val LTCMD = "ltcmd-" //永久解锁

    //电池损坏 翻修的 指令  进水
    const val DAMAGE = "inflow"

    const val SANMAXV = 4.0 //三元得最高电压是4.05

    const val SANMINV = 3.0 //三元 最低电压是2.8

    const val SNHLx0000 = "SNHLx0000-" //输入保护板密码

    const val SNHLx0001 = "SNHLx0001-" //设置租电时间


    const val SNHLx0001Success = "time-ok"


    /**
     * 传入 蓝牙地址 返回 写入蓝牙得解锁指令
     * @param address
     * @return
     */
    fun getUnLockAT(address: String): String {
        var ble = address
        val pass: Array<String>
        pass = ble.split(":".toRegex()).toTypedArray()
        ble = pass[5] + pass[4] + pass[3]
        return "$UNLOCK$ble/"
    }


    fun getNewUnlockAT(address: String): String {
        var ble = address
        val pass: Array<String>
        pass = ble.split(":".toRegex()).toTypedArray()
        ble = pass[5] + pass[4] + pass[3]
        return "$SNHLx0000$ble/"
    }


    /**
     * 传入蓝牙地址 返回永久解锁密码
     * @param address
     * @return
     */
    fun gteLtcmd(address: String): String {
        var ble = address
        val pass: Array<String>
        pass = ble.split(":".toRegex()).toTypedArray()
        ble = pass[5] + pass[4] + pass[3]
        return "$LTCMD$ble/"
    }




    /**
     * 传入时间 返回 写入时间指令
     * @param time 时间
     * @return
     */
    fun getTimeAT(time: Int): String {
        return try {
            val t = time * 60
            "$TIME_$t/"
        } catch (e: Exception) {
            "$TIME_$time/"
        }
    }

    fun getNewTimeAT(time: Int): String {

        return try {
            val t = time * 60
            "$SNHLx0001$t/"
        } catch (e: Exception) {
            "$SNHLx0001$time/"
        }

    }


    /**
     * 放电开始时间65.3  90分钟时3.84   120分时 3.63   150分时3.41   180分时3.15     200分时2.69
     * 计算电量的方法
     *放电开始时间 4.0V  25分钟以后3。84V  55分钟以后 3。63    85分钟以后3。41   115分钟以后 3。15 135分钟以后完成放电  2。69
     *计算占比  总时间 135 分钟    4。0 到3。84 放电 25分钟    占比18%   30分钟 后到 3。63  占比22%    30分钟以后 到3。41 占比 22%   30分钟以后3。15 22%  20分钟以后放电完成 14%  2%的精度缺失 补到第一个阶段
     *   4.0-3.84 电量的20%  3.84-3.63 电量的22%  3.63-3.41电量的22%  3.41-3.15电池的电量22% 3.15 到 2.7 电量的14%  全都向上增加 0.5
     *
     *  4.05 100% （0。15） 3.90 80%   3.68 58%  3.45 36% 3.20 14%  2.80  0%  保证最低电压 最低电压提高0。1
     *
     */

    /**
     * 放电开始时间65.3  90分钟时3.84   120分时 3.63   150分时3.41   180分时3.15     200分时2.69
     * 计算电量的方法
     * 放电开始时间 4.0V  25分钟以后3。84V  55分钟以后 3。63    85分钟以后3。41   115分钟以后 3。15 135分钟以后完成放电  2。69
     * 计算占比  总时间 135 分钟    4。0 到3。84 放电 25分钟    占比18%   30分钟 后到 3。63  占比22%    30分钟以后 到3。41 占比 22%   30分钟以后3。15 22%  20分钟以后放电完成 14%  2%的精度缺失 补到第一个阶段
     * 4.0-3.84 电量的20%  3.84-3.63 电量的22%  3.63-3.41电量的22%  3.41-3.15电池的电量22% 3.15 到 2.7 电量的14%  全都向上增加 0.5
     *
     * 4.05 100% （0。15） 3.90 80%   3.68 58%  3.45 36% 3.20 14%  2.80  0%  保证最低电压 最低电压提高0。1
     * 4.05 100% 3.975 90% 3.90 80%  3.80 70%  3.741  60% 3.57 50% 3.48 40% 3.38 30% 3.26 20% 3.08 10%
     *
     *
     *        30%        40%       20%        10%
     * 4.05 -----> 3.75 ----->3.5 ------>3.35----->3.2 --->0%
     *
     */
    /**
     *
     * 传入电池最低电压
     * @param min  最低电压1
     * @return 返回当前电量 百分比
     */
    fun getElectricQuantity(min: Double): Int {
        if (min >= SANMAXV) return 100
        if (min < SANMINV) return 0
        //第一个区间 电量计算 电量基数 70% 区间总占比30%
        val pr: Double = when {
            min >= 3.9 -> {
                if (min == 3.9) return 95
                val t: Double = SANMAXV - 3.9
                val v: Double = SANMAXV - min
                (1 - v / t) * 5 + 95
            }
            min >= 3.85 -> {
                if (min == 3.85) return 90
                val t: Double = 3.90 - 3.85
                val v: Double = 3.90 - min
                (1 - v / t) * 5 + 90
            }
            min >= 3.80 -> {
                if (min == 3.80 ) return 85
                val t: Double = 3.85 - 3.80
                val v: Double = 3.85 - min
                (1 - v / t) * 5 + 85
            }
            min >= 3.75 -> {
                if (min == 3.75) return 80
                val t: Double = 3.80 - 3.75
                val v: Double = 3.80 - min
                (1 - v / t) * 5 + 80
            }
            min >= 3.7 -> {
                if (min == 3.7) return 75
                val t: Double = 3.75 - 3.7
                val v: Double = 3.75 - min
                (1 - v / t) * 5 + 75
            }
            min >= 3.65 -> {
                if (min == 3.65) return 70
                val t: Double = 3.7 - 3.65
                val v: Double = 3.7 - min
                (1 - v / t) * 5 + 70
            }
            min >= 3.60 -> {
                if (min == 3.60) return 60
                val t: Double = 3.65 - 3.60
                val v: Double = 3.65 - min
                (1 - v / t) * 10 + 60
            }
            min >= 3.55 -> {
                if (min == 3.55) return 50
                val t: Double = 3.60 - 3.55
                val v: Double = 3.60 - min
                (1 - v / t) * 10 + 50
            } min >= 3.50-> {
                if (min == 3.50) return 40
                val t: Double = 3.55 - 3.50
                val v: Double = 3.55 - min
                (1 - v / t) * 10 + 40
            }min >= 3.45-> {
                if (min == 3.45) return 30
                val t: Double = 3.50 - 3.45
                val v: Double = 3.50 - min
                (1 - v / t) * 10 + 30
            }min >= 3.40-> {
                if (min == 3.40) return 25
                val t: Double = 3.45 - 3.40
                val v: Double = 3.45 - min
                (1 - v / t) * 5 + 25
            }min >= 3.3-> {
                if (min == 3.3) return 20
                val t: Double = 3.40 - 3.3
                val v: Double = 3.40 - min
                (1 - v / t) * 5 + 20
            }
            min >= SANMINV -> {
                if (min == SANMINV) return 0
                val t: Double = 3.3 - SANMINV
                val v: Double = 3.3 - min
                (1 - v / t) * 20
            }

            else -> {
                return 0
            }
        }
        return pr.toInt()
    }


    /**
     * 精确 压差
     *
     * @param v1
     * @param v2
     * @return
     */
    fun sub(v1: String?, v2: String?): Double {
        val b1 = BigDecimal(v1)
        val b2 = BigDecimal(v2)
        return b1.subtract(b2).toDouble()
    }

    /**
     * 求Map<K></K>,V>中Value(值)的最小值
     */



}