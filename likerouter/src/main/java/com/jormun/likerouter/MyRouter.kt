package com.jormun.likerouter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dalvik.system.DexFile
import java.lang.ref.WeakReference

class MyRouter {

    companion object {
        val sInstance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            MyRouter()
        }
    }

    private val routeMap = mutableMapOf<String, Class<out Activity>>()
    lateinit var context: WeakReference<Context>

    fun addActivity(path: String, activityClass: Class<out Activity>) {
        routeMap[path] = activityClass
    }

    fun getRouterMap():MutableMap<String, Class<out Activity>>{
        return routeMap
    }

/*    fun doInit(context: Context) {
        this.context = WeakReference(context)
        //val classNames = getClassName("com.your.haha")
        val classNames = ClassUtils.getFileNameByPackageName(this.context.get(), "com.your.haha")
        try {
            for (cName in classNames) {
                val clazz = Class.forName(cName)
                if (IRouter::class.java.isAssignableFrom(clazz)) {
                    val iRouter = clazz.newInstance() as IRouter
                    iRouter.putActivity()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }*/

    fun doInit() {}


    fun jumpActivity(context: Context, key: String?, bundle: Bundle?) {
        val aClass: Class<out Activity?> = routeMap[key] ?: return
        val intent = Intent(context, aClass)
        if (bundle != null) {
            intent.putExtras(bundle)
        }
        context.startActivity(intent)
    }

    private fun getClassName(packageName: String): List<String> {
        val classList = mutableListOf<String>()
        try {
            val df = DexFile(context.get()?.packageCodePath)
            val entries = df.entries()
            while (entries.hasMoreElements()) {
                val className = entries.nextElement() as String
                if (className.contains(packageName)) {
                    classList.add(className)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return classList
    }
}