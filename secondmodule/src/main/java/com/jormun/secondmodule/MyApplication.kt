package com.jormun.secondmodule

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.jormun.likeshadow.PluginManagerImpl

class MyApplication : Application() {

    companion object {
        lateinit var appContext: Context
        private const val TAG: String = "JormunPlayGroundSecondApplication"
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        //MyRouter.sInstance.doInit()
    }
}