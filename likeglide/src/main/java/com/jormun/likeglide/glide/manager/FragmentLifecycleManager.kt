package com.jormun.likeglide.glide.manager

import java.util.Collections
import java.util.WeakHashMap

/**
 * 最古老的方式，就是通过塞入Fragment来监听回调的方式
 * 这个类是负责管理这些Listener的。
 * Fragment一旦发生生命周期回调，就会调用这个类。
 * 这个类就会把自身列表里面已经注册的监听者全部通知一遍。
 * 非常标准的注册-监听模式。
 */
class FragmentLifecycleManager : LifecycleListenerManager {

    //监听者列表，需要监听的要注册进去。
    private val lifecycleListeners =
        Collections.newSetFromMap(WeakHashMap<LifecycleListener, Boolean>())

    //两个状态标记
    private var isStarted = false
    private var isDestroyed = false

    //注册到监听者列表。
    override fun addListener(listener: LifecycleListener) {
        lifecycleListeners.add(listener)
        //手动同步已有的状态。
        if (isStarted) {
            listener.onStart()
        } else if (isDestroyed) {
            listener.onDestroy()
        } else {
            listener.onStop()
        }
    }

    override fun removeListener(listener: LifecycleListener) {
        lifecycleListeners.remove(listener)
    }

    /**
     * 下面这三个方法都是给Fragment回调用的
     */
    fun onStart() {
        isStarted = true
        lifecycleListeners.forEach { listener ->
            listener.onStart()
        }
    }

    fun onStop() {
        isStarted = false
        lifecycleListeners.forEach { listener ->
            listener.onStop()
        }
    }

    fun onDestroy() {
        isDestroyed = true
        lifecycleListeners.forEach { listener ->
            listener.onDestroy()
        }
    }
}