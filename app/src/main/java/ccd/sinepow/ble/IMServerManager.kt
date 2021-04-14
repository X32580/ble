package ccd.sinepow.ble

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

/**
 * 作者 :  叶鹏
 * 时间 :  2019/12/14 8:58BLEServiceBLEService
 * 邮箱 :  1632502697@qq.com
 * 简述 :  蓝牙连接管理类
 * 更新 :
 * 时间 :
 */
class IMServerManager(context: Context) {

    lateinit var bleservice :SocketService.SocketBind
    private  var serviceConnection :ServiceConnection

    init {
        serviceConnection =object :ServiceConnection{

            override fun onServiceDisconnected(name: ComponentName?) {

            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
              bleservice  = service as SocketService.SocketBind  //得到服务对外的接口
            }
        }

        context.bindService(Intent(context,SocketService::class.java),serviceConnection,Context.BIND_AUTO_CREATE)

    }


    //静态 管理对象
    companion object {
       private  var imServerManager: IMServerManager? = null

        fun getInstance():IMServerManager{
            return imServerManager!!
        }


        fun initBleServer(context: Context){
            imServerManager = IMServerManager(context)
        }

    }




}