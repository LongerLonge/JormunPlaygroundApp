package com.jormun.playground.glidetest

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.jormun.likeglide.glide.LikeGlide
import com.jormun.likeglide.glide.request.RequestOptions
import com.jormun.playground.R
import java.lang.reflect.Array
import java.util.Arrays

class GlideTestActivity : AppCompatActivity() {

    private val TAG = "GlideTestActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glide_test)

    }

    val source = "12345"
    val dest = "ABCDE"

    fun bindAppContext(view: View) {
        //LikeGlide.with(this.applicationContext)
    }

    fun bindNewContext(view: View) {
        //LikeGlide.with(this)
    }

    fun jumpFragmentBind(view: View) {
        startActivity(Intent(this, OldFragmentActivity::class.java))
    }

    fun loadImage(view: View) {
        //LikeGlide.with(this).load("https://img0.baidu.com/it/u=2613978045,1007394376&fm=253&fmt=auto&app=120&f=JPEG?w=1280&h=800").into(findViewById<ImageView>(R.id.iv_haha))
        /*LikeGlide.with(this)
            .load("https://img1.baidu.com/it/u=4075477421,1318153425&fm=253&fmt=auto&app=120&f=JPEG?w=200&h=200")
            .into(findViewById<ImageView>(R.id.iv_haha))*/
        LikeGlide.with(this).load("https://img0.baidu.com/it/u=1203044936,4224352377&fm=253&fmt=auto&app=120&f=JPEG?w=1280&h=800").into(findViewById<ImageView>(R.id.iv_haha))
        //这张是超大图
        /*LikeGlide.with(this).load("https://s2.best-wallpaper.net/wallpaper/5120x2880/1901/Anime-girl-sit-on-desktop_5120x2880.jpg")
            .apply(RequestOptions().placeholder(R.drawable.ic_launcher_background))
            .into(findViewById<ImageView>(R.id.iv_haha))*/
    }
}