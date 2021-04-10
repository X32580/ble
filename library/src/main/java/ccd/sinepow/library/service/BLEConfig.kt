package ccd.sinepow.library.service


/**
 * 作者 ： yp
 * 时间 ： 2020/8/25
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ：
 * 更新 ：
 */
class BLEConfig {

    //连接的蓝牙服务
    var BLE_SPP_Service = Identification.BLE_SPP_Service

    //蓝牙 通知描述值
    var BLE_SPP_Notify_Characteristic =  Identification.BLE_SPP_Notify_Characteristic

    var BLE_SPP_Write_Characteristic = Identification.BLE_SPP_Write_Characteristic

    var CLIENT_CHARACTERISTIC_CONFIG = Identification.CLIENT_CHARACTERISTIC_CONFIG


    //连接超时时间
    var connectTimeOut = 15000L
    //连接 间隔时间 同时决定 连接成功回调的缓冲时间
    var intervalTime = 1000L

    var isDebug = false



}