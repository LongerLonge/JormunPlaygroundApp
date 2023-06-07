package com.jormun.likeshadow_core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

open class ShadowContext : ContextTheme {
    lateinit var pluginProxyActivity: Activity

    override fun attach(proxyActivity: Activity) {
        pluginProxyActivity = proxyActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {

    }

    fun setContentView(layoutResId: Int) {
        pluginProxyActivity.setContentView(layoutResId)
    }

    fun setContentView(rootView: View) {
        pluginProxyActivity.setContentView(rootView)
    }

    override fun onStart() {
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onStop() {
    }

    override fun onDestroy() {
    }

    override fun finish() {
    }

    override fun onSaveInstanceState(outState: Bundle?) {
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun onBackPressed() {
    }

    override fun requestWindowFeature(flag: Int) {
        pluginProxyActivity.requestWindowFeature(flag)
    }

    override fun onNewIntent(intent: Intent) {
    }

    override fun setIntent(intent: Intent) {
        pluginProxyActivity.intent = intent
    }

    override fun onLowMemory() {
    }

    override fun onTrimMemory(level: Int) {
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return false
    }

    /*override fun testSetContext(context: Context, context2: Context) {

    }*/
}