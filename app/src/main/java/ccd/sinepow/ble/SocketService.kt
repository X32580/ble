package ccd.sinepow.ble

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import tools.AppLogUtil
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.Executors

/**
 * 作者 ： yp
 * 时间 ： 2021/4/13
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ： Socket 连接服务类
 * 更新 ：
 * 注意 ：
 */
class SocketService : Service() {

    //网络线程
    val executors = Executors.newSingleThreadExecutor()

    // 连接对象
    lateinit var socket: Socket


    override fun onBind(intent: Intent?): IBinder? {
        return SocketBind()
    }


    /**
     * 连接服务器
     */
    fun connectService(address: String, port: Int): Boolean {
        AppLogUtil.e("连接到服务器$address")
        try {

            if (this::socket.isInitialized) { //已经初始化，检查是否为目标地址

                AppLogUtil.e("连接到服务器$address")

                return if (socket.isClosed) { //连接 已关闭了 重新获取地址 然后连接

                    socket.connect(socket.remoteSocketAddress)

                    if (socket.isConnected){
                        AppLogUtil.e("连接成功")
                        readMessage()
                    }else{
                        AppLogUtil.e("连接失败")
                    }

                    socket.isConnected


                } else {
                    socket.isConnected
                }
            } else {

                socket = Socket(address, port) //建立 连接对象

                if (socket.isConnected){
                    AppLogUtil.e("连接成功")
                    readMessage()
                }else{
                    AppLogUtil.e("连接失败")
                }

                return socket.isConnected

            }

        } catch (e: Exception) {
            AppLogUtil.e("连接失败 $e")
            return false

        }

    }


    /**
     * 发送数据到服务器
     * 禁止 发送空数据
     */
    private lateinit var outputStream: OutputStream

    fun sendMessage(data: String): Boolean {


        try {

            if (this::socket.isInitialized) { //已经初始化，检查是否为目标地址

                return if (socket.isConnected) {

                    outputStream = socket.getOutputStream()

                    outputStream.write(data.toByteArray())

                    outputStream.flush()

                    AppLogUtil.e("发送成功")

                    true

                } else {

                    false
                }


            } else {
                return false
            }

        } catch (e: Exception) {
            AppLogUtil.e("发送信息失败。$e")
            return false
        }


    }


    /**
     * 使用线程池  读取服务器数据
     */
    private lateinit var inputStream: InputStream
    private lateinit var bufferedReader: BufferedReader
    private  var messageString :String =""

   private fun readMessage() {

        executors.execute {

            while (true){

                if (this::socket.isInitialized) {

                    try {

                        if (socket.isConnected) {

                            inputStream = socket.getInputStream()

                            //去除 无数据的 读取
                            if (inputStream.available() < 1)
                                continue

                            bufferedReader = InputStreamReader(inputStream).buffered()
                            messageString = ""
                            while (bufferedReader.ready()) {
                                messageString += bufferedReader.read().toChar()
                            }

                            AppLogUtil.e("消息体 $messageString")

                        } else {
                            //未连接 不获取  数据
                            return@execute
                        }

                    } catch (e: Exception) {
                        AppLogUtil.e("读取信息出错$e")
                        return@execute

                    }


                } else {
                    // 未初始化 取消读取信息
                    return@execute
                }

            }


        }


    }


    /**
     * 对外 接口
     */
    inner class SocketBind : Binder() {

        /**
         * 调用连接目标 返回是否成功
         */
        fun connect(address: String, port: Int): Boolean {
            return connectService(address, port)
        }

        /**
         * 断开 连接
         */
        fun close() {
            if (this@SocketService::socket.isInitialized) {
                socket.close()
            }
        }

        /**
         * 发送信息
         */
        fun send(data: String): Boolean {
            return sendMessage(data)
        }

    }


}