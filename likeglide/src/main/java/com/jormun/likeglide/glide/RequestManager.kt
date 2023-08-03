package com.jormun.likeglide.glide

import android.util.Log
import com.jormun.likeglide.glide.manager.LifecycleListener
import com.jormun.likeglide.glide.manager.LifecycleListenerManager
import com.jormun.likeglide.glide.request.Request
import com.jormun.likeglide.glide.request.RequestBuilder
import com.jormun.likeglide.glide.request.RequestTrack
import java.io.File

/**
 * 最重要的类。
 * 负责创建图片的加载任务类，在这里是Request。
 * 负责生命周期回调的监听和处理。
 * 以上都不是它本身实现的功能，而是用了我们反复强调的组合的方式去实现。
 * 换句话说就是切割出不同的业务单元类，然后这个类负责整合它们来实现对外提供功能。
 * 我们已经不止一次说了这么做的好处了，请一定要记住。
 */
class RequestManager(
    private val glideContext: GlideContext,
    private val lifecycleListenerManager: LifecycleListenerManager
) : LifecycleListener {
    private val TAG = "RequestManager"
    //任务追踪管理类，简单点说就是负责任务的添加执行停止销毁等。
    private val requestTrack = RequestTrack()

    init {
        //让RequestManager自己注册到生命周期监听者列表里面去，这样才能获得生命周期的回调通知
        lifecycleListenerManager.addListener(this)
    }

    /**
     * 获取并构建RequestBuilder。
     * RequestBuilder负责根据传入的参数构建Request。
     *
     */
    fun load(string: String): RequestBuilder {
        return RequestBuilder(glideContext, this).load(string)
    }

    fun load(file: File): RequestBuilder {
        return RequestBuilder(glideContext, this).load(file)
    }

    /**
     * 真正处理生命周期函数回调的地方，也是执行各自周期业务的地方。
     */
    override fun onStart() {
        //执行全部任务
        Log.e(TAG, "onStart: RequestManager")
        resumeAllRequest()
    }

    private fun resumeAllRequest() {
        requestTrack.resumeRequests()
    }

    override fun onStop() {
        //暂停所有任务
        Log.e(TAG, "onStop: RequestManager")
        pauseAllRequest()
    }

    /**
     * 暂停所有任务
     */
    private fun pauseAllRequest() {
        requestTrack.pauseRequests()
    }

    /**
     * 销毁
     */
    override fun onDestroy() {
        //
        Log.e(TAG, "onDestroy: RequestManager")
        //把自己从生命周期监听者中移除
        lifecycleListenerManager.removeListener(this)
        //清除所有加载任务
        requestTrack.clearRequests()
    }

    fun track(request: Request) {
        requestTrack.runRequest(request)
    }


}