package com.jormun.likeglide.glide

import android.app.ActivityManager
import android.content.Context
import com.jormun.likeglide.glide.cache.ArrayPool
import com.jormun.likeglide.glide.cache.LruArrayPool
import com.jormun.likeglide.glide.cache.LruMemoryCache
import com.jormun.likeglide.glide.cache.MemoryCache
import com.jormun.likeglide.glide.cache.take.DiskCache
import com.jormun.likeglide.glide.cache.take.DiskLruCacheWrapper
import com.jormun.likeglide.glide.load.Engine
import com.jormun.likeglide.glide.load.GlideExecutor
import com.jormun.likeglide.glide.recycle.BitmapPool
import com.jormun.likeglide.glide.recycle.LruBitmapPool
import com.jormun.likeglide.glide.request.RequestOptions
import java.util.concurrent.ThreadPoolExecutor
import kotlin.math.roundToInt

/**
 * 负责初始化Glide，同时自身也需要处理一大堆初始化的行为。
 * 可以理解为Glide负责初始化的部分被分割出来作为一个单独类使用。
 */
class GlideBuilder {

    companion object {
        fun getMaxSize(activityManager: ActivityManager): Int {
            val memoryClassBytes = activityManager.memoryClass * 1024 * 1024
            return (memoryClassBytes * 0.4f).roundToInt()
        }
    }

    var defaultRequestOptions: RequestOptions = RequestOptions()
    lateinit var bitmapPool: BitmapPool
    lateinit var diskCache: DiskCache
    lateinit var memoryCache: MemoryCache
    lateinit var arrayPool: ArrayPool
    lateinit var engine: Engine
    lateinit var threadPoolExecutor: ThreadPoolExecutor

    /**
     * 负责构建Glider
     */
    fun build(context: Context): LikeGlide {

        //在build中初始化RequestManagerRetriever
        val requestManagerRetriever = RequestManagerRetriever()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        //Glide缓存最大可用内存大小
        val maxSize = getMaxSize(activityManager)
        if (!this::arrayPool.isInitialized)
            arrayPool = LruArrayPool()
        //减去数组缓存后的可用内存大小
        val availableSize = maxSize - arrayPool.getMaxSize()
        val displayMetrics = context.resources.displayMetrics
        // 获得一个屏幕大小的argb所占的内存大小
        val screenSize = (displayMetrics.widthPixels * displayMetrics.heightPixels) * 4

        //bitmap复用占 4份
        var bitmapPoolSize = screenSize * 4.0f
        //内存缓存占 2份
        var memoryCacheSize = screenSize * 2.0f

        if (bitmapPoolSize + memoryCacheSize <= availableSize) {
            bitmapPoolSize = Math.round(bitmapPoolSize).toFloat()
            memoryCacheSize = Math.round(memoryCacheSize).toFloat()
        } else {
            //把总内存分成 6分
            val part = availableSize / 6.0f
            bitmapPoolSize = Math.round(part * 4).toFloat()
            memoryCacheSize = Math.round(part * 2).toFloat()
        }


        if (!this::bitmapPool.isInitialized)
            bitmapPool = LruBitmapPool(bitmapPoolSize.toInt())
        if (!this::memoryCache.isInitialized)
            memoryCache = LruMemoryCache(memoryCacheSize.toInt())
        if (!this::diskCache.isInitialized)
            diskCache = DiskLruCacheWrapper(context)
        if (!this::threadPoolExecutor.isInitialized)
            threadPoolExecutor = GlideExecutor.newExecutor()

        if (!this::engine.isInitialized)
            engine = Engine(diskCache, bitmapPool, memoryCache, threadPoolExecutor)
        memoryCache.setResourceRemoveListener(engine)

        return LikeGlide(
            context,
            requestManagerRetriever,
            defaultRequestOptions,
            memoryCache,
            bitmapPool,
            arrayPool,
            engine
        )
    }
}