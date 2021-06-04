package ccd.sinepow.library.service

import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import ccd.sinepow.library.service.BleState.BLUETOOTH_NOT_ON
import ccd.sinepow.library.service.BleState.BluetoothWifiError
import ccd.sinepow.library.service.BleState.FIND_PASSAGEWAY
import ccd.sinepow.library.service.BleState.FIND_SERVICE
import ccd.sinepow.library.service.BleState.GATT_SERVICE_NULL
import ccd.sinepow.library.service.BleState.NO_FIND_SERVICE
import ccd.sinepow.library.service.BleState.START_CONNECT
import ccd.sinepow.library.service.BleState.STATE_CONNECTED
import ccd.sinepow.library.service.BleState.STATE_CONNECTING
import ccd.sinepow.library.service.BleState.STATE_DISCONNECTED
import ccd.sinepow.library.service.BleState.STATE_DISCONNECTING
import ccd.sinepow.library.service.BleState.WRITE_PASSAGEWAY
import ccd.sinepow.library.service.BleState.WRITE_PASSAGEWAY_SUCCESSFUL
import tools.AppLogUtil
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * 作者 ： yp
 * 时间 ： 2020/8/24
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ： 蓝牙服务封装 多个蓝牙同时连接
 * 更新 ：
 */
class BLEService : Service() {

    private val TAG = "蓝牙服务测试日志"

    /**
     * 蓝牙操作对象
     */
    private var bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    /**
     * 读写 对象
     */
    private lateinit var mNotifyCharacteristic: BluetoothGattCharacteristic
    private lateinit var mWriteCharacteristic: BluetoothGattCharacteristic

    /**
     * 处理业务 线程池
     */
    private lateinit var sendMessage: ExecutorService
    private lateinit var sendSignal: ExecutorService


    //连接的蓝牙服务
    private var BLE_SPP_Service = Identification.BLE_SPP_Service

    //蓝牙 通知描述值
    private var BLE_SPP_Notify_Characteristic = Identification.BLE_SPP_Notify_Characteristic

    private var BLE_SPP_Write_Characteristic = Identification.BLE_SPP_Write_Characteristic

    private lateinit var UUID_BLE_SPP_NOTIFY: UUID

    private var CLIENT_CHARACTERISTIC_CONFIG = Identification.CLIENT_CHARACTERISTIC_CONFIG


    //连接间隔时间
    private var connectInterval = 1000L

    //蓝牙超时时间
    private var connectTimeOut = 10000L

    //是否开启debug
    private var isDebug = true

    //上次连接时间
    private var oldTime = System.currentTimeMillis()

    //界面回调
    private lateinit var callBack: BLECallBack

    //蓝牙连接对象集合
    private val deviceMap = mutableMapOf<String, BleDevice>()

    //默认回调 用于 清除回调
    private val defaultCall = object : BLECallBack {
        override fun connectSuccessful(address: String) {
            log("默认回调蓝牙连接成功蓝牙地址：${address}")

        }

        override fun disConnect(address: String) {
            log("默认回调蓝牙连接断开蓝牙地址：${address}")
        }

        override fun onResult(device: BleDevice, result: String) {
            log("默认回调获取到蓝牙数据：$result")
        }

        override fun onError(address: String, code: Int) {
            log("默认回调蓝牙连接出错蓝牙地址：${address} 错误状态 ：$code")
        }

    }

    /**
     * 蓝牙 gatt 回调 所有连接对象都可以使用 这一个 用不同的 蓝牙地址区分设备
     */
    private val bleCallBack = object : BluetoothGattCallback() {
        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.let {
                    callBack.readSignal(rssi, it.device.address)
                }
            }
        }


        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val at = String(characteristic!!.value)
            log("写入数据:$at 目标蓝牙：${gatt!!.device.address}")

            //移除已写入的指令
            deviceMap[gatt!!.device.address]?.let {
                it.atList.remove(at)
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) { //成功获取当前设备


                val service = gatt!!.getService(UUID.fromString(BLE_SPP_Service))
                if (service != null) {
                    deviceMap[gatt.device.address]?.let {
                        it.state = FIND_SERVICE
                    }
                    //找到服务，继续查找特征值
                    //2.1
                    mNotifyCharacteristic =
                        service.getCharacteristic(UUID.fromString(BLE_SPP_Notify_Characteristic))
                    //2.2 特征值
                    mWriteCharacteristic =
                        service.getCharacteristic(UUID.fromString(BLE_SPP_Write_Characteristic))
                    deviceMap[gatt.device.address]?.let {
                        it.state = FIND_PASSAGEWAY
                    }


                } else {

                    deviceMap[gatt.device.address]?.let {
                        it.state = NO_FIND_SERVICE
                        it.call.onError(gatt.device.address, it.state)
                        gatt?.close()
                    }

                }


                log("特征值找到 准备写入特征值")

                //写入特征值才能通讯
                setCharacteristicNotification(mNotifyCharacteristic, true, gatt)

                //如果获取为空 则重新获取一个特征值 特征值
                mWriteCharacteristic =
                    service!!.getCharacteristic(UUID.fromString(BLE_SPP_Notify_Characteristic))


            }
        }


        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                deviceMap[gatt!!.device.address]?.let { bleDevice ->
                    bleDevice.state = WRITE_PASSAGEWAY_SUCCESSFUL

                    bleDevice.stopCountTimer()

                    log("特征值写入完成${gatt.device.address}  缓冲蓝牙$connectInterval 毫秒 然后回调 界面 ")

                    sendMessage.execute { //延迟缓冲 不然写不进去数据
                        Thread.sleep(connectInterval)
                        bleDevice.call.connectSuccessful(bleDevice.bluetoothGatt.device.address)

                        bleDevice.atList.forEach {
                            // 将未写入的 数据写入 设备
                            writeDevice(it.value,bleDevice.bluetoothGatt.device.address,bleDevice.powerId)
                        }

                    }

                    sendSignal.execute {

                        while (bleDevice.state == WRITE_PASSAGEWAY_SUCCESSFUL) {
                            Thread.sleep(2000) //过两秒获取一次 已连接的蓝牙设备
                            deviceMap.forEach { ble ->
                                ble.value.bluetoothGatt.readRemoteRssi()
                            }
                        }

                    }


                }


            }


        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {

            sendMessage.execute {


                characteristic?.let { it1 ->

                    if (it1.value.isNotEmpty()) {

                        deviceMap[gatt!!.device.address]?.let {
                            it.call.onResult(
                                it,
                                String(it1.value).replace("\n", "").replace("/", "")
                            )
                        }


                    }


                }


            }

        }


        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

            /**
             * 连接成功
             */

            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    deviceMap[gatt!!.device.address]?.let {
                        it.state = STATE_CONNECTED
                    }
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    //此时 需要调用发现服务
                    gatt.discoverServices()

                }
                //连接断开
                BluetoothGatt.STATE_DISCONNECTED -> {

                    deviceMap[gatt!!.device.address]?.let {
                        it.state = STATE_DISCONNECTED
                        //经过测试 自动断开 还是放弃这个连接 才由用户操作重新连接更好
                        log("蓝牙断开 清除连接对象")
                        //没有找到设备的  断开
                        it.call.onError(it.bluetoothGatt.device.address, it.state)
                        it.stopCountTimer()// 移除定时器
                        it.bluetoothGatt.close()
                        deviceMap.remove(it.bluetoothGatt.device.address)
                        return
                    }
                    //如果移除 蓝牙设备断开成功以后 关闭gatt 对象 释放蓝牙占用
                    gatt.let {
                        log("移除的设备 已成功断开 释放蓝牙对象${it.device.address}")
                        it.close()
                    }

                }
                BluetoothGatt.STATE_CONNECTING -> {
                    deviceMap[gatt!!.device.address]?.let {
                        it.state = STATE_CONNECTING
                    }
                }
                BluetoothGatt.STATE_DISCONNECTING -> {
                    deviceMap[gatt!!.device.address]?.let {
                        it.state = STATE_DISCONNECTING
                    }
                }

                else -> {
                    log("蓝牙连接状况改变 未知状态$newState")
                }


            }


        }
    }

    override fun onCreate() {
        super.onCreate()
        sendMessage = Executors.newSingleThreadExecutor()
        sendSignal = Executors.newSingleThreadExecutor()
        callBack = defaultCall
    }


    /**
     * 写入特征值
     */
    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean,
        gatt: BluetoothGatt
    ) {

        UUID_BLE_SPP_NOTIFY = UUID.fromString(BLE_SPP_Notify_Characteristic)

        gatt.setCharacteristicNotification(characteristic, enabled)

        if (UUID_BLE_SPP_NOTIFY == characteristic.uuid) {

            val descriptor =
                characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG))
            descriptor.value =
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE // 设置 BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE： {0x01, 0x00}
            gatt.writeDescriptor(descriptor)// 将给定描述符的值写入关联的远程设备。

            deviceMap[gatt.device.address]?.let {
                it.state = WRITE_PASSAGEWAY
            }

        }
    }

    /**
     *  连接蓝牙
     */
    fun connectDevice(address: String, powerId: String) {

        log("连接的蓝牙 ： $address")

        //未开启蓝牙 启动开启 回调界面未开启蓝牙
        if (!bluetoothAdapter.isEnabled) {
            deviceMap.clear() // 清除 连接对象
            bluetoothAdapter.enable()
            callBack.onError(address, BLUETOOTH_NOT_ON)
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val wifiManager =
                super.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (wifiManager.isWifiEnabled) {
                log("你的手机蓝牙与Wi-Fi冲突，请关闭Wi-Fi后操作")
                wifiManager.isWifiEnabled = false
                callBack.onError(address, BluetoothWifiError)
                return
            }
        }

        val time = System.currentTimeMillis()

        /**
         * 连接调用间隔 不能低于限定间隔时间
         */
        if (time - oldTime >= connectInterval) {

            //蓝牙存在连接池中
            if (deviceMap.containsKey(address)) {

                val bleDevice = deviceMap.getValue(address)

                when (bleDevice.state) {
                    //已连接
                    WRITE_PASSAGEWAY_SUCCESSFUL -> {
                        log("已连接 直接回调成功")
                        //回调界面当前 蓝牙连接成功
                        callBack.connectSuccessful(bleDevice.bluetoothGatt.device.address)

                    }
                    STATE_DISCONNECTED -> {

                        log("连接断开 直接连接蓝牙")

                        val device = bluetoothAdapter.getRemoteDevice(address)

                        /**
                         * 6.0以上使用ble 低功耗 模式 6.0以下自动选择 模式
                         */
                        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            log("Android${Build.VERSION.SDK_INT} 使用BLE连接方式")
                            device.connectGatt(this, false, bleCallBack, TRANSPORT_LE)
                        } else {
                            log("Android${Build.VERSION.SDK_INT} 使用默认的物理连接方式")
                            device.connectGatt(this, false, bleCallBack)
                        }



                        deviceMap[address]?.let {

                            it.bluetoothGatt = gatt  // 经过 测试 断开后还是 调用新的连接方法 好用
                            bleDevice.startCountDownTimer()

                        }

                    }
                    START_CONNECT -> {

                        val device = bluetoothAdapter.getRemoteDevice(address)

                        /**
                         * 6.0以上使用ble 低功耗 模式 6.0以下自动选择 模式
                         */
                        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            log("Android${Build.VERSION.SDK_INT} 使用BLE连接方式")
                            device.connectGatt(this, false, bleCallBack, TRANSPORT_LE)
                        } else {
                            log("Android${Build.VERSION.SDK_INT} 使用默认的物理连接方式")
                            device.connectGatt(this, false, bleCallBack)
                        }

                        deviceMap[address]?.let {

                            it.bluetoothGatt = gatt  // 经过 测试 断开后还是 调用新的连接方法 好用
                            bleDevice.startCountDownTimer()

                        }
                    }


                    //连接中
                    else -> {
                        log("连接中不处理该调用")
                    }

                }


            } else {


                log("没有连接的设备 立即连接 ")

                val device = bluetoothAdapter.getRemoteDevice(address)

                /**
                 * 6.0以上使用ble 低功耗 模式 6.0以下自动选择 模式
                 */
                val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    log("Android${Build.VERSION.SDK_INT} 使用BLE连接方式")
                    device.connectGatt(this, false, bleCallBack, TRANSPORT_LE)
                } else {
                    log("Android${Build.VERSION.SDK_INT} 使用默认的物理连接方式")
                    device.connectGatt(this, false, bleCallBack)
                }

                gatt?.let {
                    val bleDevice = BleDevice(
                        connectTimeOut,
                        gatt,
                        START_CONNECT,
                        callBack
                    )
                    bleDevice.startCountDownTimer()
                    bleDevice.powerId = powerId
                    //添加到 连接队列
                    deviceMap[address] = bleDevice
                    return
                }
                callBack.onError(address, GATT_SERVICE_NULL)

            }


        } else {

            log(" 连接调用 间隔太短 自动延时过滤 ")

            //连接过于频繁 延时在操作
            sendMessage.execute {

                Thread.sleep(connectInterval)

                connectDevice(address, powerId)

            }


        }

        oldTime = System.currentTimeMillis()


    }


    /**
     * 用户设备 未连接 时 记录指令 并连接设备
     * 指令将在 连接完成后 写入改设备
     */
  private  fun connectDevice(address: String, powerId: String,context: String) {

        log("连接的蓝牙 ： $address")

        //未开启蓝牙 启动开启 回调界面未开启蓝牙
        if (!bluetoothAdapter.isEnabled) {
            deviceMap.clear() // 清除 连接对象
            bluetoothAdapter.enable()
            callBack.onError(address, BLUETOOTH_NOT_ON)
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val wifiManager =
                super.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (wifiManager.isWifiEnabled) {
                log("你的手机蓝牙与Wi-Fi冲突，请关闭Wi-Fi后操作")
                wifiManager.isWifiEnabled = false
                callBack.onError(address, BluetoothWifiError)
                return
            }
        }

        val time = System.currentTimeMillis()

        /**
         * 连接调用间隔 不能低于限定间隔时间
         */
        if (time - oldTime >= connectInterval) {


                log("开始连接设备-----> ")

                val device = bluetoothAdapter.getRemoteDevice(address)

                /**
                 * 6.0以上使用ble 低功耗 模式 6.0以下自动选择 模式
                 */
                val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    log("Android${Build.VERSION.SDK_INT} 使用BLE连接方式")
                    device.connectGatt(this, false, bleCallBack, TRANSPORT_LE)
                } else {
                    log("Android${Build.VERSION.SDK_INT} 使用默认的物理连接方式")
                    device.connectGatt(this, false, bleCallBack)
                }

                gatt?.let {
                    val bleDevice = BleDevice(
                        connectTimeOut,
                        gatt,
                        START_CONNECT,
                        callBack
                    )
                    bleDevice.startCountDownTimer()
                    bleDevice.powerId = powerId
                    //添加到 连接队列
                    deviceMap[address] = bleDevice
                    bleDevice.atList[context] = context //添加到指令集合内
                    return
                }
                callBack.onError(address, GATT_SERVICE_NULL)


        } else {

            log(" 连接调用 间隔太短 自动延时过滤 ")

            //连接过于频繁 延时在操作
            sendMessage.execute {

                Thread.sleep(connectInterval)

                connectDevice(address, powerId)

            }


        }

        oldTime = System.currentTimeMillis()


    }



    /**
     * 写入 指令到指定到设备
     */
    fun writeDevice(context: String, address: String,powerId: String) {


        // 连接存在的时候 直接写入数据 到蓝牙
        deviceMap[address]?.let { de ->

            if (de.state == WRITE_PASSAGEWAY_SUCCESSFUL) {


                mWriteCharacteristic =
                    de.bluetoothGatt.getService(UUID.fromString(BLE_SPP_Service))
                        .getCharacteristic(UUID.fromString(BLE_SPP_Write_Characteristic))
                //放入写入数据

                /**
                 * 大于20 需要分包 处理
                 * 分包处理的数据 需要更快执行
                 */

                /**
                 * 发送
                 */

                var ats = context

                while (true) {

                    if (ats.length <= 20) {

                        val at = ats.substring(0, ats.length)
                        log("写入蓝牙指令$at")
                        mWriteCharacteristic.value = at.toByteArray()

                        //写入 对象
                        de.bluetoothGatt.writeCharacteristic(mWriteCharacteristic)
                        //发送间隔 100ms
                        Thread.sleep(200)

                        return

                    } else {

                        val at = ats.substring(0, 19)
                        log("写入蓝牙指令$at")
                        ats = ats.substring(19, ats.length)
                        mWriteCharacteristic.value = at.toByteArray()

                        //写入 对象
                        de.bluetoothGatt.writeCharacteristic(mWriteCharacteristic)
                        //发送间隔 100ms
                        Thread.sleep(200)

                    }

                }


            }else{
                //将指令 记录
                de.atList[context] = context
                //调用连接 设备
                connectDevice(address, powerId)

            }
            return
        }

        // 设备没有 在连接对象池

        connectDevice(address, powerId,context)


    }


    private var isSearch = false

   private val scanCallback = object : ScanCallback() {

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            searchCall.error(errorCode)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            AppLogUtil.e("扫描到设备$result")

            if (result != null) {
                searchCall.findDevice(result.device)
            }

        }


    }

   lateinit var searchCall:BLEScannerCallback
    fun search(callback: BLEScannerCallback) {

        searchCall = callback

        if (isSearch) {
            AppLogUtil.e("扫描中 无法继续扫描")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            AppLogUtil.e("未开启蓝牙")
            bluetoothAdapter.enable()
            return
        }

        isSearch = true


        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)

        Thread.sleep(6000)

        if (isSearch)
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)

        isSearch = false

    }


    /**
     *  停止 搜索 立刻停止
     */
    fun stopSearch(){

        if (isSearch){
            isSearch = false
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }

    }


    override fun onBind(p0: Intent?): IBinder? {
        return BleBind()
    }

    inner class BleBind : Binder(), IBleBinderInterface {


        override fun isConnect(address: String): Boolean {
            deviceMap[address]?.let {
                return it.state == WRITE_PASSAGEWAY_SUCCESSFUL
            }
            return false
        }

        override fun connect(address: String, powerId: String) {
            connectDevice(address, powerId)
        }

        override fun stopSearchFun(callback: BLEScannerCallback) {
            stopSearch()
        }

        override fun writeData(data: String, address: String,powerId: String) {
            writeDevice(data, address,powerId)
        }

        override fun disConnectRetainDevice(address: String) {
            log("清除除目标蓝牙以外的设备回调并断开蓝牙 $address")
            val list: MutableList<String> = mutableListOf()
            deviceMap.forEach {
                if (it.key != address) {
                    it.value.call = defaultCall
                    it.value.atList.clear()
                    it.value.bluetoothGatt.disconnect()
                    it.value.stopCountTimer()
                    list.add(it.key)
                }
            }
            //安全的移除 这些连接对象
            list.forEach {
                deviceMap.remove(it)
            }


        }

        override fun disConnectAndRemoveCallback(address: String) {
            log("清除目标蓝牙回调并断开蓝牙 $address")
            deviceMap[address]?.let {
                it.call = defaultCall
                it.bluetoothGatt.disconnect()
                it.atList.clear()
                it.stopCountTimer()
                deviceMap.remove(address)
            }

        }


        override fun disConnectAll() {
            log("断开所有的蓝牙连接")
            var list = mutableListOf<String>()
            //清除指定 蓝牙回调
            deviceMap.forEach {
                it.value.call = defaultCall
                it.value.atList.clear()
                it.value.bluetoothGatt.disconnect()
                it.value.stopCountTimer()
                list.add(it.key)
            }

            //安全的移除对象
            list.forEach {
                deviceMap.remove(it)
            }

        }

        /**
         * 需要传入 回调者 防止错误的 清除
         */
        override fun cleanAllCall(callback: BLECallBack) {
            log("尝试清除所有蓝牙回调")
            if (this@BLEService.callBack == callback) {
                this@BLEService.callBack = defaultCall
                deviceMap.forEach {
                    it.value.call = defaultCall
                    it.value.atList.clear()

                }
            }
        }

        override fun setCallback(callback: BLECallBack) {
            log("设置界面回调$callback")
            this@BLEService.callBack = callback
            //重置回调 并且开启回调
            deviceMap.forEach {
                it.value.call = callback
            }
        }

        override fun setDebug(boolean: Boolean) {
            isDebug = boolean
        }

        override fun setConfig(config: BLEConfig) {
            this@BLEService.BLE_SPP_Notify_Characteristic = config.BLE_SPP_Notify_Characteristic
            this@BLEService.BLE_SPP_Service = config.BLE_SPP_Service
            this@BLEService.BLE_SPP_Write_Characteristic = config.BLE_SPP_Write_Characteristic
            this@BLEService.CLIENT_CHARACTERISTIC_CONFIG = config.CLIENT_CHARACTERISTIC_CONFIG
            this@BLEService.connectTimeOut = config.connectTimeOut
            this@BLEService.isDebug = config.isDebug
            this@BLEService.connectInterval = config.intervalTime
        }

        override fun searchDevice(callback: BLEScannerCallback) {
            search(callback)
        }

        //设置连接间隔时间
        override fun setTime(time: Long) {
            connectInterval = time
        }


    }

    //对外暴露的接口
    internal interface IBleBinderInterface {

        fun isConnect(address: String): Boolean

        fun connect(address: String, powerId: String)

        fun stopSearchFun(callback: BLEScannerCallback)

        //写入 指令到 设备
        fun writeData(data: String, address: String,powerId: String)

        // 断开所有设备  也会移除 连接池对象
        fun disConnectAll()

        //断开其余的设备并清除回调 只保留该设备 也会移除监听回调 和连接池对象
        fun disConnectRetainDevice(address: String)

        //断开连接 并且 清除回调   所有的 界面使用 完成之后必须啊 清除回调 移除连接池对象
        fun disConnectAndRemoveCallback(address: String)

        //清除所有蓝牙回调  需要传入监听对象防止错误清除
        fun cleanAllCall(callback: BLECallBack)

        //设置 接口回调
        fun setCallback(callback: BLECallBack)

        //设置debug 模式
        fun setDebug(boolean: Boolean)

        //设置蓝牙配置文件
        fun setConfig(config: BLEConfig)

        //搜索设备
        fun searchDevice(callback: BLEScannerCallback)


        fun setTime(time:Long)

    }

    //打印 日志
    fun log(string: String) {
        if (isDebug)
            Log.e(TAG, string)
    }

}