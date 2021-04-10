package ccd.sinepow.ble

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ccd.sinepow.library.service.AppLogUtil

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppLogUtil.e("打印日志测试")

        AppLogUtil.isDeBug = true

    }


    override fun onRestart() {
        super.onRestart()
        AppLogUtil.e("不应该出现他")
    }
}