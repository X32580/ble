package tools

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * 作者 ： yp
 * 时间 ： 2020/9/12
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ： toast 工具
 * 更新 ：
 */
object ToastUtils {

    lateinit var application: Application
    lateinit var handle : Handler
    fun showToastShort(title:String){
            handle.post {
                Toast.makeText(application,title,Toast.LENGTH_SHORT).show()
            }

    }

    fun init(application: Application){
        ToastUtils.application =application
        handle = Handler(Looper.getMainLooper())
    }


}