package com.jormun.likeglide.glide

import android.app.Fragment
import com.jormun.likeglide.glide.manager.FragmentLifecycleManager

/**
 * 塞进Activity中帮忙同步监听生命周期的Fragment。
 * 一旦产生回调，就通过FragmentLifecycleManager去通知所有注册了的监听者。
 * 最老式的方法，看到我们用的还是被废弃的Fragment。。。。连Androidx都不是。
 */
class LifecycleHelperFragment : Fragment() {
    private val TAG = "LifecycleHelperFragment"

    //监听者管理类
    var fragmentLifecycleManager: FragmentLifecycleManager = FragmentLifecycleManager()

    //虽然RequestManager被定义在了这里，实际上只是作为缓存用，Fragment并不会使用到这个类。
    private var requestManager: RequestManager? = null

    fun getRequestManager(): RequestManager? {
        return requestManager
    }

    fun setRequestManager(requestManager: RequestManager) {
        this.requestManager = requestManager
    }

    /**
     * 生命周期发生回调了，就通过管理者通知所有已经注册的监听者
     */
    override fun onStart() {
        super.onStart()
        fragmentLifecycleManager.onStart()
    }

    override fun onStop() {
        super.onStop()
        fragmentLifecycleManager.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentLifecycleManager.onDestroy()
    }
}