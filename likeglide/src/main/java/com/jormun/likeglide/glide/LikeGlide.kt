package com.jormun.likeglide.glide

import android.content.Context

/**
 * 整合各个组件，并且暴露出去对外提供功能的类。
 * 请一定要记住，不能全部功能都写在一个类，而是分割出去，通过组合的方式去实现功能。
 * internal 为同一模块下可见
 */
class LikeGlide internal constructor(
    context: Context,
    private val retriever: RequestManagerRetriever
) {

    private val glideContext: GlideContext

    init {
        //初始化上下文对象
        glideContext = GlideContext(context)
        retriever.glideContext = this.glideContext
    }


    companion object {
        //单例
        private lateinit var glide: LikeGlide

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
            if (!this::glide.isInitialized) {
                initGlide(context, GlideBuilder())
            }
            return glide
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
    }


}