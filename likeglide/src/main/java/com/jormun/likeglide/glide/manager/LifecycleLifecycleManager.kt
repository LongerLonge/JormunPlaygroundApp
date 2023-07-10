package com.jormun.likeglide.glide.manager

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * 负责监听生命周期的中介类。
 * 由传统的塞入Fragment替代为利用Android x里面的 jetpack 组件
 * 也就是LifecycleObserver来实现。
 * 里面有一个集合，来保存需要接受生命周期监听回调的对象们。
 * 一旦有生命周期发起回调，就会遍历列表通知它们。
 * 非常典型的注册-监听模式。
 */
class LifecycleLifecycleManager(private val lifecycle: Lifecycle) : LifecycleListenerManager,
    DefaultLifecycleObserver {

    private val lifecycleListeners = HashSet<LifecycleListener>()

    init {
        //LifecycleObserver的正式用法，添加addObserver即可。
        lifecycle.addObserver(this)
    }

    /**
     * 一旦发生回调，就遍历列表通知已经注册的观察者。
     */
    override fun onStart(owner: LifecycleOwner) {
        lifecycleListeners.forEach { listener ->
            listener.onStart()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        lifecycleListeners.forEach { listener ->
            listener.onStop()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        lifecycleListeners.forEach { listener ->
            listener.onDestroy()
        }
    }

    /**
     * 把监听者注册到列表中。
     */
    override fun addListener(listener: LifecycleListener) {
        lifecycleListeners.add(listener)
    }

    /**
     *移除监听者。
     */
    override fun removeListener(listener: LifecycleListener) {
        lifecycleListeners.remove(listener)
    }


}