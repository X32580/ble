package ccd.sinepow.ble

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ccd.sinepow.ble.BatteryAT.SNHLx0001Success
import ccd.sinepow.library.service.BLECallBack
import ccd.sinepow.library.service.BLEServerManager
import ccd.sinepow.library.service.BleDevice
import kotlinx.android.synthetic.main.device_changer_test.*
import tools.AppLogUtil
import tools.ToastUtils

/**
 * 作者 ： yp
 * 时间 ： 2021/8/4
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ：
 * 更新 ：
 * 注意 ：
 */
class ExchangeTestActivity :AppCompatActivity() {

    private val adapter = TestAdapter(this)

    /**
     * 定时超过 15秒就是失败
     */
    private val countDownTimer = object  : CountDownTimer(15000,1000){
        override fun onTick(millisUntilFinished: Long) {

        }

        override fun onFinish() {
            runOnUiThread {
                val explain = Explain(1,0) //超时归类为 找不到设备
                adapter.addData(explain)
                testTotal-=1
                errorCount+=1
                next()
            }

        }

    }

    var testTotal = 10 //总测试次数
    var t = 10
    var parallel = true //是否 并行执行
    var successCount = 0 //成功次数统计
    var errorCount = 0 //错误次数
    var avg = 0.0 //成功率
    var deviceList :MutableList<Device> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deviceList = TestUtils.call!!
        setContentView(R.layout.device_changer_test)
        changer_test_error_rv.layoutManager  = LinearLayoutManager(this)
        changer_test_error_rv.adapter = adapter

        changer_test_connect_type_change.setOnClickListener {
            parallel = !parallel
            if (parallel){
                changer_test_connect_type_change.text = "并行模式"
            }else{
                changer_test_connect_type_change.text = "单连模式"
            }
        }

        changer_start_test.setOnClickListener {

            BLEServerManager.cleanConnect()
            newSuccess = false
            oldSuccess = false
            errorCount = 0
            successCount = 0
            avg = 0.0
            changer_test_success_count.text = "成功次数:$successCount"
            changer_test_error_count.text = "失败次数:$errorCount"
            changer_test_success_avg.text = "成功率:$avg"
            changer_start_test.text = "正在测试"
            adapter.clean()
            try {
                testTotal = changer_test_count_ed.text.toString().toInt()
                t = testTotal
            }catch (e:Exception){
                ToastUtils.showToastShort("叼毛 不要乱输入")
                return@setOnClickListener
            }

            oldDeviceAddress = getDevice(null)
            newDeviceAddress = getDevice(oldDeviceAddress)
            countDownTimer.start()
            if (parallel){ //并行连接模式
                BLEServerManager.connect(oldDeviceAddress,"1000")
                BLEServerManager.connect(newDeviceAddress,"2000")
            }else{
                BLEServerManager.connect(oldDeviceAddress,"1000")
            }

            changer_start_test.isEnabled = false
        }

        BLEServerManager.setCall(object :BLECallBack{
            override fun connectSuccessful(address: String) {

                if (address == oldDeviceAddress)
                BLEServerManager.write("SNHLx0002/",address,"1000") //写入关锁

                if (address == newDeviceAddress){
                    BLEServerManager.write(BatteryAT.getNewUnlockAT(address),address,"1000") //写入关锁
                    BLEServerManager.write(BatteryAT.getNewTimeAT(10903),address,"1000") //写入关锁
                }

            }

            override fun disConnect(address: String) {
               AppLogUtil.e("连接断开")
            }

            override fun onResult(device: BleDevice, result: String) {

                AppLogUtil.e("蓝牙信息 回传 $result ")
                if (device.bluetoothGatt.device.address == newDeviceAddress){

                    when(result){
                        SNHLx0001Success->{
                            newSuccess = true
                            if (newSuccess && oldSuccess){
                                testTotal-=1 //通过本次测试
                                successCount+=1
                                next()
                            }
                        }

                    }

                }
                if (device.bluetoothGatt.device.address == oldDeviceAddress){
                    if (result == "back-ok"){
                        oldSuccess = true
                        if (newSuccess && oldSuccess){
                            testTotal-=1 //通过本次测试
                            successCount+=1
                            next()
                        }
                        BLEServerManager.disconnect(oldDeviceAddress)
                        if (!parallel) //单连接模式 再次连接 新电池
                            BLEServerManager.connect(newDeviceAddress,"2000")

                    }
                }

            }

            override fun onError(address: String, code: Int) {
                runOnUiThread {
                    val explain = Explain(code,1)
                    adapter.addData(explain)
                    testTotal-=1
                    errorCount+=1
                    next()
                }
            }

        })

    }

    /**
     * 开始下一次 测试
     */
    fun next(){
        countDownTimer.cancel()
        newSuccess = false
        oldSuccess = false
        avg =(successCount.toDouble()/t)*100
        BLEServerManager.cleanConnect()


        runOnUiThread {
            changer_test_success_count.text = "成功次数:$successCount"
            changer_test_error_count.text = "失败次数:$errorCount"
            changer_test_success_avg.text = "成功率:$avg"
            changer_start_test.text = "正在测试"
        }

        Thread.sleep(8000) //强制停止  过快操作蓝牙

        if (testTotal>0){

            oldDeviceAddress = getDevice(null)
            newDeviceAddress = getDevice(oldDeviceAddress)

            if (parallel){ //并行连接模式
                BLEServerManager.connect(oldDeviceAddress,"1000")
                BLEServerManager.connect(newDeviceAddress,"2000")
            }else{
                BLEServerManager.connect(oldDeviceAddress,"1000")
            }
            countDownTimer.start()
        }else{
            ToastUtils.showToastShort("测试已完成")
            runOnUiThread{
                changer_start_test.isEnabled = true
                changer_start_test.text = "开始测试"
            }
        }

    }

    var newDeviceAddress = ""
    var oldDeviceAddress = ""
    var newSuccess = false
    var oldSuccess = false

    /**
     * 随机从 数组中拿出一个蓝牙连接 需要保证 集合最低两个
     * @param address 需要过滤的地址  传null 将会完全随机 获取
     */
   private fun getDevice(address:String?):String{

        if (address !=null){
            while (true){
                val d = deviceList.random()
                if (d.address != address){
                    return d.address
                }
            }
        }
        return deviceList.random().address
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
    }

}