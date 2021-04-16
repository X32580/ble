package tools

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap

/**
 * 作者 :  叶鹏
 * 时间 :  2020/6/4 14:21
 * 邮箱 :  1632502697@qq.com
 * 简述 :  全局 异常 处理工具类
 * 更新 :
 * 时间 :
 * 版本 : V 1.0
 */
class GlobalExceptionTools(var context: Context,var exceptionCallback: (String)->Unit) :Thread.UncaughtExceptionHandler {

    // 用于存放参数信息
    private val info = LinkedHashMap<String, String>()
    // 用于格式化日期
    private val mDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())


    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }



    private fun putInfoToMap(context: Context) {
        info["设备型号"] = Build.MODEL
        info["设备品牌"] = Build.BOARD
        info["硬件名称"] = Build.HARDWARE
        info["硬件制造商"] = Build.MANUFACTURER
        info["系统版本"] = Build.VERSION.RELEASE
        info["系统版本号"] = "${Build.VERSION.SDK_INT}"
        val pm = context.packageManager
        val pi = pm.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
        if (pi != null) {
            info["应用版本"] = pi.versionName
            info["应用版本号"] = "${PackageInfoCompat.getLongVersionCode(pi)}"
        }
    }

    private fun getLogHeader(): StringBuffer {
        val sb = StringBuffer()
        sb.append("--------->>>>时间: ${mDateFormat.format(Date())}<<<<<<--------")
        putInfoToMap(context)
        info.entries.forEach {
            sb.append("${it.key}: ${it.value} ")
        }
        return sb
    }


    private fun getLogSummary(e: Throwable): String {
        val sb = getLogHeader().append("\n")
        sb.append("异常类: ${e.javaClass}\n")
        sb.append("异常信息: ${e.message}\n")
        val writer =  StringWriter()
        val pw = PrintWriter(writer)
        e.printStackTrace(pw)
        pw.close()
        // 打印出错误日志
        return sb.toString().trim()+writer.toString()
    }

    /**
     * 发生异常时 回调此方法
     */
    override fun uncaughtException(t: Thread, e: Throwable) {
        exceptionCallback.invoke(getLogSummary(e))
    }



    companion object{

        private var globalExceptionTools : GlobalExceptionTools? = null

        /**
         * 初始化全局异常处理
         */
        fun initGlobalException(context: Context,exceptionCallback: (String)->Unit){
            globalExceptionTools = GlobalExceptionTools(context,exceptionCallback)
        }

        /**
         * 获取单例 对象 处理全局异常
         */
        fun getInstance(): GlobalExceptionTools?{
            return globalExceptionTools
        }


    }



}