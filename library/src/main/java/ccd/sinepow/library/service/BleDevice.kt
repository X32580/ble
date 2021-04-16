package ccd.sinepow.library.service

import android.bluetooth.BluetoothGatt
import android.os.Handler
import android.os.Looper
import ccd.sinepow.library.service.BleState.START_CONNECT
import tools.AppLogUtil


/**
 * 作者 ： yp
 * 时间 ： 2020/8/24
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ： 蓝牙连接服务 的设备
 * 更新 ：
 */
class BleDevice(
    val time_out: Long,
    var bluetoothGatt: BluetoothGatt,
    var state: Int,
    var call: BLECallBack
) {

    lateinit var powerId: String
    private var stop = false

    //存放指令集合  未写入的指令将会存放在 集合内
    val atList: MutableMap<String, String> = mutableMapOf()

    //开始 连接 超时计时  去你妈的CountDownTimer 需要主线程 还不如handler 好用
    fun startCountDownTimer() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (stop)
                return@postDelayed
            if (state != BleState.WRITE_PASSAGEWAY_SUCCESSFUL) {

                AppLogUtil.e("蓝牙连接不成功,已经超时,当前错误代码$state")

                call.onError(bluetoothGatt.device.address, state)

                //没有连接成功  因为没有找到设备
                state = START_CONNECT

                bluetoothGatt.disconnect()
                bluetoothGatt.close()
                //没有找到设备的时候 不调用断开方法

            }
        }, time_out)

    }


    /**
     * 停止 倒计时
     */
    fun stopCountTimer() {
        stop = true
    }


}