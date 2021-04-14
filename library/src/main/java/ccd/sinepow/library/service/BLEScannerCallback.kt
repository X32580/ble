package ccd.sinepow.library.service

import android.bluetooth.BluetoothDevice

/**
 * 作者 ： yp
 * 时间 ： 4/14/21
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ： 蓝牙 扫描 回调
 * 更新 ：
 * 注意 ：
 */
interface BLEScannerCallback {
    //找到设备
    fun findDevice(device:BluetoothDevice)
    //出现错误
    fun error(code:Int)

}