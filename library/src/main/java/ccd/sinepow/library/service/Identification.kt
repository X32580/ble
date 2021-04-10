package ccd.sinepow.library.service

/**
 * 作者 :  叶鹏
 * 时间 :  2020/5/26 11:27
 * 邮箱 :  1632502697@qq.com
 * 简述 :  通道标识
 * 更新 :
 * 时间 :
 * 版本 : V 1.0
 */
object Identification {


    /**
     * 保存 蓝牙 的 通道 此通道为默认值
     */
    const val BLE_SPP_Service = "0000fee0-0000-1000-8000-00805f9b34fb"
    const val BLE_SPP_Notify_Characteristic = "0000fee1-0000-1000-8000-00805f9b34fb"
    const val BLE_SPP_Write_Characteristic = "0000fee2-0000-1000-8000-00805f9b34fb"
    const val BLE_SPP_AT_Characteristic = "0000fee3-0000-1000-8000-00805f9b34fb"
    const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"


}