package com.jormun.likeglide.glide

import android.content.Context

/**
 * 负责初始化Glide，同时自身也需要处理一大堆初始化的行为。
 * 可以理解为Glide负责初始化的部分被分割出来作为一个单独类使用。
 */
class GlideBuilder {

    /**
     * 负责构建Glider
     */
    fun build(context: Context): LikeGlide {
        //在build中初始化RequestManagerRetriever
        val requestManagerRetriever = RequestManagerRetriever()
        return LikeGlide(context, requestManagerRetriever)
    }
}