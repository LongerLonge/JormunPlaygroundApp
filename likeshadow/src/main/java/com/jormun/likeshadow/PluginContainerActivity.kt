package com.jormun.likeshadow

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.jormun.likeshadow_core.ContextTheme
import java.lang.Exception

class PluginContainerActivity : Activity() {

    // val testClassName = "com.jormun.secondmodule.SecondModuleMainActivity"
    private var contextTheme: ContextTheme? = null
    val TAG = "PluginContainerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doLoadApk()
        //setContentView(R.layout.activity_plugin_container)
    }

    private fun doLoadApk() {

        try {
            val apkPath = intent.getStringExtra("apk")
            PluginManagerImpl.sInstance.setHostContext(this)
            val launchIntentFromApk = PluginManagerImpl.sInstance.getLaunchIntentFromApk(apkPath)
            //launchIntentFromApk?.apply {
                //val className = component?.className
                val loadClass = classLoader?.loadClass("com.unity3d.player.UnityPlayerActivity")
                contextTheme = loadClass?.newInstance() as ContextTheme
                //contextTheme?.testSetContext(this@PluginContainerActivity,this@PluginContainerActivity)
                contextTheme?.attach(this@PluginContainerActivity)
                contextTheme?.onCreate(Bundle())
            //}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        contextTheme?.onResume()
    }

    override fun onStart() {
        super.onStart()
        contextTheme?.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        contextTheme?.onDestroy()
    }


    override fun onStop() {
        super.onStop()
        contextTheme?.onStop()
    }


    override fun onPause() {
        super.onPause()
        contextTheme?.onPause()
    }


    //以下都是Unity需要的：
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        contextTheme?.onNewIntent(intent!!)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        contextTheme?.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        contextTheme?.onTrimMemory(level)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        contextTheme?.onConfigurationChanged(newConfig)
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        contextTheme?.onWindowFocusChanged(hasFocus)
    }


/*    override fun getClassLoader(): ClassLoader? {
        try {
            return PluginManagerImpl.sInstance.getPluginApkDexClassLoader()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }*/

    override fun getResources(): Resources? {
        try {
            return PluginManagerImpl.sInstance.getPluginApkResources()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}