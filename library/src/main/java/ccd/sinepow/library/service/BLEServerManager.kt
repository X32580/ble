package ccd.sinepow.library.service

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.IBinder
import android.util.Log

/**
 * 作者 :  叶鹏
 * 时间 :  2019/12/14 8:58BLEServiceBLEService
 * 邮箱 :  1632502697@qq.com
 * 简述 :  蓝牙连接管理类
 * 更新 :  增加初始化
 * 时间 :
 */
class BLEServerManager(private val context: Context) {


    var bleservice: BLEService.BleBind? = null
    private var serviceConnection: ServiceConnection
    private var cuerrTime = System.currentTimeMillis()
    private var initTime = cuerrTime // 保留 启动蓝牙所需要的时间 用于重新初始化所需要的时间
    private val bluetoothLister = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {

                val state = intent.action

                if (state == BluetoothAdapter.ACTION_STATE_CHANGED) {

                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_TURNING_OFF -> { //关闭蓝牙的时候 清空当前连接池 的对象
                            bleservice?.let { ble ->
                                ble.disConnectAll()
                            }
                        }
                        BluetoothAdapter.STATE_OFF -> { //在断开之前 释放所有的连接对象 断开以后 无法操作

                        }
                    }


                }


            }


        }

    }

    init {
        serviceConnection = object : ServiceConnection {

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.e("BLEServerManager", "错误 蓝牙服务断开绑定")
            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

                service?.let {
                    bleservice = it as BLEService.BleBind  //得到服务对外的接口
                    AppLogUtil.e("BLEServerManager蓝牙绑定成功 ")
                    initTime = System.currentTimeMillis() - cuerrTime
                    AppLogUtil.e("绑定蓝牙服务所需要的时间$initTime 毫秒")
                    cuerrTime = System.currentTimeMillis()
                    //初始化蓝牙成功的时候 注册广播监听器 监听蓝牙 的状况
                    val intentFilter = IntentFilter()
                    intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                    context.registerReceiver(bluetoothLister,intentFilter) //注册 监听蓝牙的状况
                    val bleConfig = BLEConfig()
                    bleConfig.intervalTime = BLEConfig().intervalTime //获取蓝牙的连接速度
                    bleConfig.isDebug = true
                    bleservice?.setConfig(bleConfig)
                    return
                }
                //没有绑定成功 重新绑定
                onResumeInit()
            }
        }

        //绑定服务
        context.bindService(
            Intent(context, BLEService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

    }

    /**
     * 服务绑定失败
     */
    private fun onResumeInit() {
        AppLogUtil.e("错误 蓝牙服务未建立 重新新建立蓝牙服务")
        val t = System.currentTimeMillis()
        if (t - cuerrTime > initTime + 1500) { //每次初始化 时间 间隔为1。5秒加第一次初始化时间
            cuerrTime = t
            if (bleservice != null) {
                AppLogUtil.e("已经初始化，取消本次初始化蓝牙操作")
                return
            }
            context.bindService(
                Intent(context, BLEService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        } else {
            AppLogUtil.e("过快调用 服务重新绑定 自动过滤")
        }

    }


    //静态 管理对象
    companion object {
        private var bleServerManager: BLEServerManager? = null


        /**
         * 连接指定设备
         */
        fun connect(address: String, powerId: String) {
            bleServerManager?.let {

                it.bleservice?.let { blebind ->
                    blebind.connect(address, powerId)
                    return
                }

                AppLogUtil.e("蓝牙未初始化,自动重新构建")
                it.onResumeInit()

            }
        }

        /**
         * 修复 蓝牙为初始化 造成的闪退
         * 写入方法 由 本单例 执行
         */
        fun write(content: String, address: String) {

            bleServerManager?.let {

                it.bleservice?.let { blebind ->
                    blebind.writeData(content, address)
                    return
                }

                AppLogUtil.e("蓝牙未初始化,自动重新构建")
                it.onResumeInit()

            }


        }

        /**
         * 断开指定设备 并清除回调
         */
        fun disconnect(address: String) {
            bleServerManager?.let {

                it.bleservice?.let { blebind ->
                    blebind.disConnectAndRemoveCallback(address)
                    return
                }
                AppLogUtil.e("蓝牙未初始化,自动重新构建")
                it.onResumeInit()

            }
        }

        /**
         * 断开所有连接
         */
        fun cleanConnect() {
            bleServerManager?.let {

                it.bleservice?.let { blebind ->
                    blebind.disConnectAll()
                    return
                }
                AppLogUtil.e("蓝牙未初始化,自动重新构建")
                it.onResumeInit()
            }
        }

        /**
         * 设置监听器
         */
        fun setCall(call: BLECallBack) {
            bleServerManager?.let {

                it.bleservice?.let { blebind ->
                    blebind.setCallback(call)
                    return
                }
                AppLogUtil.e("蓝牙未初始化,自动重新构建")
                it.onResumeInit()
            }
        }

        /**
         * 清除 所有的 监听会滴
         */
        fun cleanCall(call: BLECallBack) {
            bleServerManager?.let {

                it.bleservice?.let { blebind ->
                    blebind.cleanAllCall(call)
                    return
                }
                AppLogUtil.e("蓝牙未初始化,自动重新构建")
                it.onResumeInit()
            }
        }


        /**
         * 断开除该设备 以外的设备
         */
        fun disConnectRetainDevice(address: String) {
            bleServerManager?.let {

                it.bleservice?.let { blebind ->
                    blebind.disConnectRetainDevice(address)
                    return
                }
                AppLogUtil.e("蓝牙未初始化,自动重新构建")
                it.onResumeInit()
            }
        }

        /**
         * 检查目标是否 连接
         */
        fun isConnect(address: String): Boolean {
            bleServerManager?.let {

                it.bleservice?.let { blebind ->
                    return blebind.isConnect(address)
                }
                AppLogUtil.e("蓝牙未初始化,自动重新构建")
                it.onResumeInit()
            }
            return false
        }

        /**
         * 设置间隔时间
         */
        fun setTime(time:Long){
            bleServerManager?.let {
                it.bleservice?.let { blebind ->
                    val bleConfig = BLEConfig()
                    bleConfig.intervalTime =time
                    blebind.setConfig(bleConfig)
                    return
                }
                AppLogUtil.e("蓝牙未初始化,自动重新构建")
                it.onResumeInit()
            }
        }

        /**
         * 设置debug
         */
        fun setDebug(boolean: Boolean){
            bleServerManager?.let {
                it.bleservice?.let { blebind ->
                 blebind.setDebug(boolean)
                    return
                }
                AppLogUtil.e("蓝牙未初始化,自动重新构建")
                it.onResumeInit()
            }
        }

        fun initBleServer(context: Context) {
            if (bleServerManager ==null)
            bleServerManager = BLEServerManager(context)
        }

    }


}