package com.jormun.likeglide.glide.load

import android.util.Log
import com.jormun.likeglide.glide.GlideContext
import com.jormun.likeglide.glide.bean.Key
import com.jormun.likeglide.glide.bean.Resources
import com.jormun.likeglide.glide.cache.ActiveResource
import com.jormun.likeglide.glide.cache.MemoryCache
import com.jormun.likeglide.glide.cache.take.DiskCache
import com.jormun.likeglide.glide.recycle.BitmapPool
import com.jormun.likeglide.glide.request.ResourceCallback
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 负责整合数据加载层的各个类工作的管理者。
 * 组装了数据加载层的一堆类。
 * 比如，活动缓存，内存缓存，磁盘缓存，线程池等等。
 * 负责协调这些组件工作，比如开启图片网络或本地的加载并且接收回调，活动缓存内存缓存的移除和添加等等。
 * 可以说是数据加载层的发动机。
 *@param diskCache 顾名思义，磁盘缓存对象 三级缓存之一
 * @param bitmapPool Bitmap复用池，用来存放从内存缓存被移除(超出大小)的对象
 * @param memoryCache 顾名思义，内存缓存对象 三级缓存之一
 * @param threadPoolExecutor 线程池，给每个图片加载都创建一个线程，丢进线程池里面执行
 */
class Engine(
    //磁盘缓存
    var diskCache: DiskCache,
    //Bitmap复用池
    var bitmapPool: BitmapPool,
    //内存缓存
    var memoryCache: MemoryCache,
    //线程池
    var threadPoolExecutor: ThreadPoolExecutor,
) : MemoryCache.ResourceRemoveListener, Resources.ResourceListener, EngineJob.EngineJobListener {
    private val TAG = "Engine"

    /**
     * Engine加载状态封装类，封装了一个EngineJob，并返回给上层调用者。
     * 调用者可以根据情况来决定是否关闭这个Job。
     */
    class LoadStatus(
        private var resourceCallback: ResourceCallback,
        private var engineJob: EngineJob
    ) {
        /**
         * 调用者手动关闭任务。
         */
        fun cancel() {
            engineJob.removeCallback(resourceCallback)
            // TODO: 看看这里是否需要关闭
            engineJob.cancel()
        }
    }

    //活动缓存，三级缓存之一
    private var activeResource: ActiveResource = ActiveResource(this)

    //任务列表，每个EngineJob都包了一个DecodeJob，可以看作是每个可执行的图片加载子线程
    //注意是以EngineKey为区分
    val jobs = mutableMapOf<Key, EngineJob>()

    /**
     * 释放内存
     * 关闭线程池
     * 清除所有缓存对象的数据
     */
    fun shutDown() {
        val shutdownSeconds: Long = 5
        threadPoolExecutor.shutdown()
        try {
            //尝试在5S内关闭所有线程
            if (!threadPoolExecutor.awaitTermination(shutdownSeconds, TimeUnit.SECONDS)) {
                threadPoolExecutor.shutdown()
                if (!threadPoolExecutor.awaitTermination(shutdownSeconds, TimeUnit.SECONDS)) {
                    throw Exception("Failed to shutdown")
                }
            }

        } catch (e: Exception) {
            throw Exception(e.message)
        }
        diskCache.clear()
        activeResource.destroy()
        Log.e(TAG, "shutDown: 关闭所有加载线程成功")
    }

    /**
     * 开始执行加载图片任务。
     *  @param model 输入的数据，可以是一串url或者文件地址
     * @param width 宽
     * @param height 高
     * @param cb 回调，调用者传入来作为回调通知
     */
    fun load(
        glideContext: GlideContext,
        model: Any,
        width: Int,
        height: Int,
        cb: ResourceCallback
    ): LoadStatus? {
        //根据传入的参数创建EngineKey，EngineKey只用来在这个类里进行标记
        val engineKey = EngineKey(model, width, height)
        //第一步、先从活跃缓存中找
        val actResource = activeResource.getActiveResource(engineKey)
        //不为空，则是找到
        actResource?.apply {
            Log.e(TAG, "load: 从活跃缓存中找到了！ $this")
            acquire()//因为被用到了，注意计数要+1
            cb.onResourceReady(this)
            return null
        }
        //第二步、从内存缓存里面找
        //之所以用remove不用get，是因为我们假如找到的话需要从内存缓存移除然后添加到活动缓存，用get就做不到一次性完成
        //用remove就可以一次性完成获取和移除操作
        val memoryResource = memoryCache.removeResource(engineKey)
        //不为空则是找到
        memoryResource?.apply {
            Log.e(TAG, "load: 从内存缓存里面找到了！ $this")
            //加入到活动缓存
            activeResource.activeResource(engineKey, this)
            acquire()//因为被用到了，计数+1
            //对这个Resource添加监听，因为如果这个Resource从活动缓存中被移除了我们需要拿到它然后加回到内存缓存中
            //见 onResourceReleased()
            setResourceListener(engineKey, this@Engine)
            cb.onResourceReady(this)
            return null
        }

        //第三步、磁盘缓存或者网络IO
        //尝试从jobs里面找
        val engineJob = jobs[engineKey]
        //不为空，则说明找到，这个图片已经进入加载队列
        engineJob?.apply {
            Log.e(TAG, "load: 没有缓存，数据正在加载，添加监听等待回调！")
            //添加个监听等待完成回调就行了
            addCallback(cb)
            return LoadStatus(cb, engineJob)
        }

        //以上都没有的话，就说明需要新建一个加载job了
        Log.e(TAG, "load: 这数据第一次被加载，创建加载任务并加入队列！")
        //创建EngineJob，设置监听
        // 这里监听者有两位，一位是Engine(需要获得Bitmap操作缓存)，一个是Request(需要获得Bitmap使用)
        val newEngineJob = EngineJob(threadPoolExecutor, engineKey, this)
        newEngineJob.addCallback(cb)
        //创建一个DecodeJob，EngineJob负责指挥DecodeJob工作
        val decodeJob = DecodeJob(glideContext, diskCache, model, width, height, newEngineJob)
        //由Engine来启动DecodeJob
        newEngineJob.start(decodeJob)
        //把这个job缓存起来
        jobs[engineKey] = newEngineJob
        //把EngineJob封装到LoadStatus里面去，让上层调用者一个可以关闭的入口。
        return LoadStatus(cb, newEngineJob)

    }

    //----------------这个类真正属于自己干活的也就上面这两个方法，剩下的都是接收的回调-----------------------

    /**
     * 引用计数变0，把Bitmap抛出去
     * 从活动缓存移除，让内存缓存保存
     */
    override fun onResourceReleased(key: Key, resources: Resources) {
        Log.e(TAG, "引用计数为0,移除活跃缓存，加入内存缓存:$key")
        activeResource.deactivateResource(key)
        memoryCache.put(key, resources)
    }

    /**
     * 内存缓存超出大小被抛出的Bitmap
     * 随后加入到复用池里面去
     */
    override fun onResourceRemove(resources: Resources) {
        Log.e(TAG, "内存缓存移除，加入复用池")
        bitmapPool.put(resources.bitmap)
    }

    /**
     * EngineJob的加载结果回调
     */
    override fun onEngineJobComplete(engineJob: EngineJob, key: Key, resources: Resources?) {
        //添加到活跃缓存里
        resources?.apply {
            setResourceListener(key, this@Engine)
            activeResource.activeResource(key, this)
        }
        //完成了就从队列移除
        jobs.remove(key)
    }

    /**
     * EngineJob的移除通知回调
     */
    override fun onEngineJobCancelled(engineJob: EngineJob, key: Key) {
        Log.e(TAG, "onEngineJobCancelled: 关闭job：${key}")
        jobs.remove(key)
    }


}