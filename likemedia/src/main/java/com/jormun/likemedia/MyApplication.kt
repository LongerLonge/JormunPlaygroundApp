package com.jormun.likemedia

import android.app.Application
import android.content.Context

class MyApplication: Application() {
    companion object {
        lateinit var appContext: Context
        private const val TAG: String = "JormunPlayGroundApplication"
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}