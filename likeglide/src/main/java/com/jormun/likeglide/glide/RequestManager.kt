package com.jormun.likeglide.glide

import android.util.Log
import com.jormun.likeglide.glide.manager.LifecycleListenerManager
import com.jormun.likeglide.glide.manager.LifecycleListener

/**
 * 最重要的类。
 * 负责图片的请求和加载。
 * 负责生命周期回调的监听和处理。
 * 以上都不是它本身提供的功能，而是用了我们反复强调的组合的方式去实现。
 * 换句话说就是切割出不同的业务单元类，然后这个类负责整合它们来实现对外提供功能。
 * 我们已经不止一次说了这么做的好处了，请一定要记住。
 */
class RequestManager(lifecycleListenerManager: LifecycleListenerManager) : LifecycleListener {
    private val TAG = "RequestManager"

    init {
        //让RequestManager自己注册到监听者列表里面去
        lifecycleListenerManager.addListener(this)
    }

    /**
     * 真正处理生命周期函数回调的地方，也是执行各自周期业务的地方。
     */
    override fun onStart() {
        //执行真正的业务。。。
        Log.e(TAG, "onStart: RequestManager")
    }

    override fun onStop() {
        //执行真正的业务。。。
        Log.e(TAG, "onStop: RequestManager")
    }

    override fun onDestroy() {
        //执行真正的业务。。。
        Log.e(TAG, "onDestroy: RequestManager")
    }
}