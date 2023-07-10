package com.jormun.likeglide.glide.manager


/**
 * 添加或者移除生命周期监听的管理类顶层行为接口
 */
interface LifecycleListenerManager {
    fun addListener(listener: LifecycleListener)
    fun removeListener(listener: LifecycleListener)
}