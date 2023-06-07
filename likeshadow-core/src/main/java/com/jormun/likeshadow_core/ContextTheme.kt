package com.jormun.likeshadow_core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent


interface ContextTheme {

    fun attach(proxyActivity: Activity)

    fun onCreate(savedInstanceState: Bundle?)
    fun onStart()
    fun onResume()

    fun onPause()

    fun onStop()

    fun onDestroy()
    fun finish()
    fun onSaveInstanceState(outState: Bundle?)

    fun onTouchEvent(event: MotionEvent?): Boolean

    fun onBackPressed()

    //下面这些都是UnityActivity用到的：

    fun requestWindowFeature(flag: Int)

    fun onNewIntent(intent: Intent)
    fun setIntent(intent: Intent)

    fun onLowMemory()

    fun onTrimMemory(level: Int)

    fun onConfigurationChanged(newConfig: Configuration)

    fun onWindowFocusChanged(hasFocus: Boolean)

    fun dispatchKeyEvent(event: KeyEvent): Boolean

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean
    fun onGenericMotionEvent(event: MotionEvent): Boolean


}