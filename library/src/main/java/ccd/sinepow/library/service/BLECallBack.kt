package ccd.sinepow.library.service

/**
 * 作者 ： yp
 * 时间 ： 2020/7/27
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ： 蓝牙回调 接口
 * 更新 ：
 */
interface BLECallBack {

    //连接成功
    fun connectSuccessful(address:  String )
    //连接断开
    fun disConnect(address: String)
    //返回 电池数据
    fun onResult(device: BleDevice, result :String)
    //获取蓝牙信号
    fun readSignal(signal :Int,address: String){}
    //蓝牙连接出错 超时也会走这个回调
    fun onError(address: String ,code : Int)

}