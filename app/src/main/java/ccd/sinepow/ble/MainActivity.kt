package ccd.sinepow.ble

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import tools.AppLogUtil
import ccd.sinepow.library.service.BLEScannerCallback
import ccd.sinepow.library.service.BLEServerManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppLogUtil.isDeBug = true

        BLEServerManager.initBleServer(applicationContext)


        IMServerManager.initBleServer(this)


        send_message.setOnClickListener {


                Thread{

                    BLEServerManager.search(object : BLEScannerCallback{
                        override fun findDevice(device: BluetoothDevice) {
                            AppLogUtil.e("发现设备$device")
                        }

                        override fun error(code: Int) {
                            AppLogUtil.e("扫描出错$code")
                        }

                    },this)

//                    IMServerManager.getInstance().bleservice.send(edit_input.text.toString())

                }.start()



        }

        connect.setOnClickListener {


//            Thread{
//
//                IMServerManager.getInstance().bleservice.connect("192.168.0.114",7878)
//
//            }.start()

        }

        text_11.setOnClickListener {

            BLEServerManager.stopSearch(object : BLEScannerCallback{
                override fun findDevice(device: BluetoothDevice) {
                    AppLogUtil.e("发现设备$device")
                }

                override fun error(code: Int) {
                    AppLogUtil.e("扫描出错$code")
                }

            },this)

        }



    }






}