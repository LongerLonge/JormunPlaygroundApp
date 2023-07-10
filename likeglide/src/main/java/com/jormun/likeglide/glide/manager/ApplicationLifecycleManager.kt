package com.jormun.likeglide.glide.manager

/**
 * 监听Application也就是app进程的生命周期类。
 * 因为本身所有内存和数据都是跟着app同时存活的，所以根本不需要做什么特殊的操作。
 */
class ApplicationLifecycleManager : LifecycleListenerManager {
    override fun addListener(listener: LifecycleListener) {
        //强行开启Start就可以了，其它不需要做。
        listener.onStart()
    }

    override fun removeListener(listener: LifecycleListener) {

    }
}