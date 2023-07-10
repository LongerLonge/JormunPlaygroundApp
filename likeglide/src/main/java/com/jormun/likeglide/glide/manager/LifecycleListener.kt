package com.jormun.likeglide.glide.manager

/**
 * 进行生命周期回调的顶层抽象类
 */
interface LifecycleListener {
    fun onStart()
    fun onStop()
    fun onDestroy()
}