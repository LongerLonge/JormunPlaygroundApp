package com.jormun.playground

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import com.jormun.MainViewModel
import com.jormun.likeroom.DbTestActivity
import com.jormun.likerouter.MyRouter
import com.jormun.likerouter_annotation.Route
import com.jormun.playground.rftest.ApiServersTest
import com.jormun.retrofit.RetrofitMock

@Route("main/main")
class MainActivity : AppCompatActivity() {

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainViewModel = defaultViewModelProviderFactory.create(MainViewModel::class.java)

    }

    fun testRetrofit(view: View) {
        mainViewModel.testRf().observe(this, Observer { result ->
            Toast.makeText(this@MainActivity, "请求完成", Toast.LENGTH_SHORT).show()
            findViewById<TextView>(R.id.tv_rf_show).text = result
        })
    }

    fun routerDb(view: View) {
        MyRouter.sInstance.jumpActivity(this, "db/test", null)
        //finish()
    }
}