package ccd.sinepow.library.service


/**
 * 作者 ： yp
 * 时间 ： 2020/8/23
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ： 存放 蓝牙连接状态
 * 更新 ：
 */
object BleState {

    //未连接
    const val NOT_CONNECT = 0
    //连接开始
    const val START_CONNECT= 1
    //蓝牙连接中
    const val STATE_CONNECTING = 2
    //连接成功
    const val STATE_CONNECTED =3
    //没有找到服务
    const val NO_FIND_SERVICE =4
    //找到服务
    const val FIND_SERVICE = 5
    //找到通道
    const val FIND_PASSAGEWAY = 6
    //没有找到通道
    const val NO_FIND_PASSAGEWAY = 7
    //开始写入特征值
    const val WRITE_PASSAGEWAY = 8
    //写入完成
    const val WRITE_PASSAGEWAY_SUCCESSFUL = 9
    //蓝牙断开中
    const val STATE_DISCONNECTING = 10
    //蓝牙断开成功
    const val STATE_DISCONNECTED = 11
    //蓝牙 连接对象为null
    const val GATT_SERVICE_NULL = 12
    //蓝牙未打开
    const val BLUETOOTH_NOT_ON =13
    //低版本手机 蓝牙与Wi-Fi冲突错误
    const val BluetoothWifiError = 15




}