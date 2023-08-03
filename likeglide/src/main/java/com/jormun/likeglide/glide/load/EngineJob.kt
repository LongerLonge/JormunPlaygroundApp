package com.jormun.likeglide.glide.load

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.jormun.likeglide.glide.bean.Key
import com.jormun.likeglide.glide.bean.Resources
import com.jormun.likeglide.glide.request.ResourceCallback
import java.util.concurrent.ThreadPoolExecutor

/**
 * 协调并指挥DecodeJob执行的类，真正加载数据的是DecodeJob并不是这个类。
 * 等待DecodeJob完成回调，并且把需要监听的监听者注册到这里。
 * 同时这个类还负责在DecodeJob(子线程)加载完后切换到主线程，通过Handler来切换。
 * @param threadPoolExecutor 因为每个DecodeJob都是一个runnable，所以需要指定一个线程池来执行。
 * @param engineKey 因为Engine层是用EngineKey来标记的，所以需要接收一个EngineKey来回调给Engine定位操作。
 * @param engineJobListener 监听EngineJob的接口，也是完成状态的回调，通过这个接口通知监听者。
 *                          在这个例子监听者就为Engine。
 */
class EngineJob(
    private val threadPoolExecutor: ThreadPoolExecutor,
    private var engineKey: EngineKey?,
    private val engineJobListener: EngineJobListener
) : DecodeJob.DecodeJobCallback {

    private val TAG = "EngineJob"

    companion object {
        //Handler的what，用来区别状态
        private const val MSG_COMPLETE = 1
        private const val MSG_EXCEPTION = 2
        private const val MSG_CANCELLED = 3

        //这里暂时用Handler来实现吧
        private val MAIN_THREAD_HANDLER = Handler(Looper.getMainLooper(), MainThreadCallback())
    }

    //成功从EngineJob加载并解码的Bitmap
    private var resource: Resources? = null

    private var isCancelled = false//是否关闭加载
    private var decodeJob: DecodeJob? = null//图片加载的任务类，真正执行加载行为的对象

    //一个job(图片)可能需要被多处地方使用，就需要一个列表在装下多个监听者
    //在这里的例子就是一个EngineJob可能被多个Request监听。
    private val cbs = mutableListOf<ResourceCallback>()

    /**
     * Handler的实现类，暂时用这个实现。
     * 加载行为通过线程池在子线程执行，完成后通过Handler切换回主线程。
     * 线程的切换在Android很重要，请一定要记住。
     */
    private class MainThreadCallback : Handler.Callback {
        override fun handleMessage(message: Message): Boolean {
            //只要执行到这里的，都已经切换回主线程
            val job: EngineJob = message.obj as EngineJob
            when (message.what) {
                MSG_COMPLETE -> job.handleResultOnMainThread()
                MSG_EXCEPTION -> job.handleExceptionOnMainThread()
                MSG_CANCELLED -> job.handleCancelledOnMainThread()
                else -> throw IllegalStateException("Unrecognized message: " + message.what)
            }
            return true
        }
    }

    /**
     * 完成，在主线程处理并回调
     */
    private fun handleResultOnMainThread() {
        //如果已经关闭任务直接释放即可
        if (isCancelled) {
            resource?.recycle()
            release()
            return
        }
        //不为空
        resource?.apply {
            //将引用计数+1
            acquire()
            //EngineJob实际上被两个地方监听，如下：
            //1、Engine监听，通知让其去操作缓存
            engineJobListener.onEngineJobComplete(this@EngineJob, engineKey!!, this)
            //2、Request监听，cbs里面是ResourceCallback集合，这里其实就是一堆的Request。
            for (cb in cbs) {
                //每多一个回调者，都要引用计数 +1
                acquire()
                //通知Request图片加载完了
                cb.onResourceReady(resource)
            }
            release()
            this@EngineJob.release()
        }
    }

    /**
     * 错误处理（已经切换至主线程）
     */
    private fun handleExceptionOnMainThread() {
        if (isCancelled) {
            release()
            return
        }
        //回调通知
        engineKey?.apply {
            engineJobListener.onEngineJobComplete(this@EngineJob, this, null)
        }
    }

    /**
     * 取消处理（已经切换至主线程）
     */
    private fun handleCancelledOnMainThread() {
        //回调通知
        engineKey?.apply {
            engineJobListener.onEngineJobCancelled(this@EngineJob, this)
        }
    }

    /**
     * 监听接口
     */
    interface EngineJobListener {
        fun onEngineJobComplete(engineJob: EngineJob, key: Key, resources: Resources?)
        fun onEngineJobCancelled(engineJob: EngineJob, key: Key)
    }

    /**
     * 正式开始任务
     * DecodeJob是一个runnable(子线程)，可以丢进线程池里面运行
     */
    fun start(decodeJob: DecodeJob) {
        this.decodeJob = decodeJob
        threadPoolExecutor.execute(decodeJob)
    }

    /**
     * 添加监听, 这个监听是给需要用这个图片显示的地方监听
     * 比如Request
     */
    fun addCallback(cb: ResourceCallback) {
        Log.e(TAG, "设置加载状态监听")
        cbs.add(cb)
    }

    /**
     * 移除监听
     */
    fun removeCallback(cb: ResourceCallback) {
        Log.e(TAG, "移除加载状态监听")
        cbs.remove(cb)
        //这一个请求取消了，可能还有其他地方的请求
        //只有回调为空 才表示请求需要取消
        if (cbs.isEmpty()) {
            cancel()
        }
    }

    /**
     * 关闭job，同时如果回调接口不为空就通过接口通知
     */
    fun cancel() {
        isCancelled = true
        decodeJob?.apply {
            cancel()
        }
        engineKey?.apply {
            engineJobListener.onEngineJobCancelled(this@EngineJob, this)
        }
    }

    /**
     * 释放，都置空
     */
    private fun release() {
        cbs.clear()
        engineKey = null
        resource = null
        isCancelled = false
        decodeJob = null
    }

    override fun onResourceReady(resources: Resources) {
        this.resource = resources
        //成功，通过Handler切回主线程并且通知
        MAIN_THREAD_HANDLER.obtainMessage(MSG_COMPLETE, this).sendToTarget()
    }

    override fun onLoadFailed(e: Exception) {
        //失败，同样通过Handler来切回主线程并且通知
        MAIN_THREAD_HANDLER.obtainMessage(MSG_EXCEPTION, this).sendToTarget()
    }


}