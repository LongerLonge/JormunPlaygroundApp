package com.jormun.playground

import android.app.Application
import android.content.Context
import com.jormun.likerouter.MyRouter

class MyApplication : Application() {

    companion object {
        lateinit var appContext: Context
        private const val TAG: String = "JormunPlayGroundApplication"
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        MyRouter.sInstance.doInit()
    }
}