package com.jormun.likeroom

import android.app.Application
import android.content.Context

class RoomApplication : Application() {

    companion object {
        lateinit var appContext: Context
        private const val TAG: String = "RoomApplication"
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}