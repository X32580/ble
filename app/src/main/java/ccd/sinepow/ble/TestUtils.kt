package ccd.sinepow.ble

import android.content.Context
import android.content.Intent

/**
 * 作者 ： yp
 * 时间 ： 2021/8/4
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ：
 * 更新 ：
 * 注意 ：
 */
object TestUtils  {

     var call:MutableList<Device>? =null

    fun startTestActivity(data:MutableList<Device>,context: Context){
        this.call = data
        context.startActivity(Intent(context,ExchangeTestActivity::class.java))
    }

}