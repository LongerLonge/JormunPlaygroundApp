package com.jormun.playground

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.jormun.likerouter.MyRouter
import com.jormun.likerouter_annotation.Route
import com.jormun.playground.glidetest.GlideTestActivity
import com.jormun.playground.rctest.RcvTestStaggeredActivity
import com.jormun.playground.splugin.PluginApkTestActivity

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
        Glide.with(this).load("").into(ImageView(this))
    }

    fun testPluginApk(view: View) {
        startActivity(Intent(this, PluginApkTestActivity::class.java))
    }

    fun testGlide(view: View) {
        startActivity(Intent(this, GlideTestActivity::class.java))
    }

    fun testRcv01(view: View) {startActivity(Intent(this, RcvTestStaggeredActivity::class.java))}
}