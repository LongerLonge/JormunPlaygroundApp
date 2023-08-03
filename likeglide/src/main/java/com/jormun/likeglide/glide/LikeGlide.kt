package com.jormun.likeglide.glide

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import com.jormun.likeglide.glide.cache.ArrayPool
import com.jormun.likeglide.glide.cache.MemoryCache
import com.jormun.likeglide.glide.load.Engine
import com.jormun.likeglide.glide.load.codec.StreamBitmapDecoder
import com.jormun.likeglide.glide.load.model.FileLoader
import com.jormun.likeglide.glide.load.model.FileUriLoader
import com.jormun.likeglide.glide.load.model.HttpUriLoader
import com.jormun.likeglide.glide.load.model.StringModelLoader
import com.jormun.likeglide.glide.recycle.BitmapPool
import com.jormun.likeglide.glide.request.RequestOptions
import java.io.File
import java.io.InputStream

/**
 * 对外提供API的类，可以看作是入口类。
 * 本身不实现功能(或只实现本类必须的少部分功能)，而且暴露出api给用户。
 * 用户调用API函数后，返回正确的对象给用户，让用户可以正常使用。
 * internal 为同一模块下可见
 */
class LikeGlide internal constructor(
    context: Context,
    private val retriever: RequestManagerRetriever,
    private val requestOptions: RequestOptions,
    private val memoryCache: MemoryCache,
    private val bitmapPool: BitmapPool,
    private val arrayPool: ArrayPool,
    val engine: Engine
) : ComponentCallbacks2 {

    private val glideContext: GlideContext

    init {

        //注册数据加载器Loader、解码器Decoder
        val registry = Registry()
        val contentResolver = context.contentResolver
        registry
            .add(String::class.java, InputStream::class.java, StringModelLoader.Factory())
            .add(Uri::class.java, InputStream::class.java, HttpUriLoader.Factory())
            .add(Uri::class.java, InputStream::class.java, FileUriLoader.Factory(contentResolver))
            .add(File::class.java, InputStream::class.java, FileLoader.Factory())
            .register(InputStream::class.java, StreamBitmapDecoder(bitmapPool, arrayPool))
        //初始化上下文对象
        glideContext = GlideContext(context, requestOptions, registry, engine)
        //塞到retriever里面去
        retriever.glideContext = this.glideContext
    }


    companion object {
        //单例
        private var glide: LikeGlide? = null

        /**
         * Glide的入口，做了两件事
         * 1、获取RequestManagerRetriever
         * 2、从RequestManagerRetriever中获取RequestManager
         */
        fun with(context: Context): RequestManager {
            return getRetriever(context).get(context)
        }

        /**
         * 获取RequestManagerRetriever，做了两件事
         * 1、初始化Glide(如果有必要的话)
         * 2、初始化RequestManagerRetriever
         */
        private fun getRetriever(context: Context): RequestManagerRetriever {
            return get(context).retriever
        }

        //同步锁创建LikeGlide单例对象并返回
        @Synchronized
        private fun get(context: Context): LikeGlide {
            if (glide == null) {
                initGlide(context, GlideBuilder())
            }
            return glide!!
        }

        /**
         * 初始化Glide，暴露出去可以让使用者使用自己的builder来构建Glide。
         * 同时让Builder初始化了RequestManagerRetriever
         */
        fun initGlide(context: Context, glideBuilder: GlideBuilder) {
            val applicationContext = context.applicationContext
            //是由builder来构建的，要注意。
            //RequestManagerRetriever也是在里面构建的，注意。
            val likeGlide = glideBuilder.build(applicationContext)
            glide = likeGlide
        }

        /**
         * 没内存了，要被干掉了，直接释放全部内容
         */
        @Synchronized
        fun tearDown() {
            glide?.apply {
                glideContext.context.unregisterComponentCallbacks(glide)
                engine.shutDown()
                glide = null
            }
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {

    }

    /**
     * 内存紧张
     */
    override fun onLowMemory() {
        //memory和bitmappool顺序不能变
        //因为memory移除后会加入复用池
        memoryCache.clearMemory()
        bitmapPool.clearMemory()
        arrayPool.clearMemory()
    }

    /**
     * 需要释放内存
     *在onTrimMemory 中可以根据系统的内存状况及时调整App内存占用，提升用户体验或让App存活更久
     * @param level 内存状态
     */
    override fun onTrimMemory(level: Int) {
        memoryCache.trimMemory(level)
        bitmapPool.trimMemory(level)
        arrayPool.trimMemory(level)
    }


}