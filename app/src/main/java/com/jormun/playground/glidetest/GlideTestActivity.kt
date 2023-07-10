package com.jormun.playground.glidetest

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.jormun.likeglide.glide.LikeGlide
import com.jormun.playground.R

class GlideTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glide_test)

    }



    fun bindAppContext(view: View) {
        LikeGlide.with(this.applicationContext)
    }
    fun bindNewContext(view: View) {
        LikeGlide.with(this)
    }
    fun jumpFragmentBind(view: View) {
        startActivity(Intent(this, OldFragmentActivity::class.java))
    }
}