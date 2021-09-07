package ccd.sinepow.ble

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ccd.sinepow.library.service.BLECallBack
import tools.AppLogUtil
import ccd.sinepow.library.service.BLEScannerCallback
import ccd.sinepow.library.service.BLEServerManager
import ccd.sinepow.library.service.BleDevice
import kotlinx.android.synthetic.main.activity_main.*
import tools.ToastUtils

class MainActivity : AppCompatActivity() {

    private val adapter = DeviceAdapter(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppLogUtil.isDeBug = true

        device_rv.layoutManager = LinearLayoutManager(this)

        device_rv.adapter = adapter

        BLEServerManager.initBleServer(applicationContext)

        ToastUtils.init(application)

        button_search.setOnClickListener {

            Thread{
                BLEServerManager.search(object : BLEScannerCallback {
                    override fun findDevice(device: BluetoothDevice) {
                        AppLogUtil.e("发现设备$device")

                        if (device.name!=null){
                            runOnUiThread {
                                val device = Device(device.name, device.address, false)
                                adapter.addDevice(device)
                            }
                        }

                    }

                    override fun error(code: Int) {
                        AppLogUtil.e("扫描出错$code")
                    }

                }, this)

            }.start()

        }

        button_changer_test.setOnClickListener {

            BLEServerManager.setCall(object :BLECallBack{
                override fun connectSuccessful(address: String) {

                    BLEServerManager.write("SNHLx0000-123456/","01:68:56:83:09:63","1000")


                }

                override fun disConnect(address: String) {

                }

                override fun onResult(device: BleDevice, result: String) {
                    AppLogUtil.e("device ${device.powerId} result $result")
                }

                override fun onError(address: String, code: Int) {

                }

            })

            BLEServerManager.setDebug(true)

            BLEServerManager.connect("01:68:56:83:09:63","1000")


//            BLEServerManager.stopSearch(object : BLEScannerCallback {
//                override fun findDevice(device: BluetoothDevice) {
//                    AppLogUtil.e("发现设备$device")
//                    runOnUiThread {
//                        val device = Device(device.name, device.address, false)
//                        adapter.addDevice(device)
//                    }
//                }
//
//                override fun error(code: Int) {
//                    AppLogUtil.e("扫描出错$code")
//                }
//
//            }, this)
//
//            val data = adapter.getEnableDevice()
//            if (data.size >= 2) {
//                TestUtils.startTestActivity(data,this)
//                startActivity(Intent(this,ExchangeTestActivity::class.java))
//            }else{
//                ToastUtils.showToastShort("测试至少需要2个电池")
//            }

        }


    }


}