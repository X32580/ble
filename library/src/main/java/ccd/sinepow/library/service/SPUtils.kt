package ccd.sinepow.library.service

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

/**
 * 作者 :  叶鹏
 * 时间 :  2020/4/27 11:41
 * 邮箱 :  1632502697@qq.com
 * 简述 : SharedPreferences 工具类 读取本地文件 注意 float 默认返回值 为100
 * 更新 :
 * 时间 :
 * 版本 : V 1.0
 */
class SPUtils (context: Context)  {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(context.packageName+"SPUtils",Context.MODE_PRIVATE)


    companion object{

         lateinit var spUtils  :SPUtils

        /**
         * 记得初始化 sp工具类
         */
        fun initSp(context: Context){
                  spUtils = SPUtils(context)

                }

        fun getSP():SPUtils?{

            return spUtils
        }


    }

    fun  getString(key : String): String? {
        return sharedPreferences.getString(key,null)
    }
    fun getInt(key :String ):Int{
        return sharedPreferences.getInt(key,0)
    }
    fun getBoolean(key:String):Boolean{
        return sharedPreferences.getBoolean(key,false)
    }
    fun getFloat(key: String):Float{
        return sharedPreferences.getFloat(key,0.0f)
    }
    fun getLong(key: String):Long{
        val data  = sharedPreferences.getString(key, "100") ?: return  100L
        return data.toLong()
    }
    fun getAll():Map<String,*>{
        return sharedPreferences.all
    }

    fun put(key: String,data :Any){
        val editor = sharedPreferences.edit()
        when (data){
            data is String->{
                editor.putString(key,data.toString())
            }
            data is Int->{
                editor.putInt(key,data as Int)
            }
            data is Float->{
                editor.putFloat(key,data as Float)
            }
            data is Long ->{
                editor.putLong(key,data as Long)
            }
            data is Boolean->{
                editor.putBoolean(key,data as Boolean)
            }
            else->{
                editor.putString(key,data.toString())
            }
        }
        editor.apply()
    }

    private fun getAuto(key: String, data: Any?):Any?{
        when (data){
            data is String->{
                return sharedPreferences.getString(key,null)
            }
            data is Int->{
                return sharedPreferences.getInt(key,0)
            }
            data is Float->{
                return sharedPreferences.getFloat(key,0.0f)
            }
            data is Long ->{
               return sharedPreferences.getLong(key,0L)
            }
            data is Boolean->{
               return sharedPreferences.getBoolean(key,false)
            }
            else->{
                return sharedPreferences.getString(key,null)            }
        }
    }


    fun remove(key: String){
        sharedPreferences.edit().remove(key).apply()
    }
    fun  removeAll(){
        sharedPreferences.edit().clear().apply()
    }

    fun containsKey(key: String):Boolean{
        return sharedPreferences.contains(key)
    }


    fun containsKey(vararg  key: String):Boolean{
        key.forEach {
           if (!sharedPreferences.contains(it))
               return false
        }
        return true
    }

    /**
     * 保存对象
     */
    fun <T>saveObject(key: String,data:T){
        val gson = Gson()
        val gs = gson.toJson(data)
        put(key,gs)
    }

    /**
     *   取出对象
     */
    fun <T> getObject(key: String,data :Class<T>):T{
        val gson = Gson()
        val string = getAuto(key,data).toString()
        return gson.fromJson(string,data)
    }



}


