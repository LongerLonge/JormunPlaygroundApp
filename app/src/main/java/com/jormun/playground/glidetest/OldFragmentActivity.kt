package com.jormun.playground.glidetest

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.jormun.playground.R

class OldFragmentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_old_fragment)
    }

    fun fragmentBind(view: View) {
        //LikeGlide.with(this)
    }
}