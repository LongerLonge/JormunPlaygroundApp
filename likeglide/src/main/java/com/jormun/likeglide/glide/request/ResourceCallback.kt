package com.jormun.likeglide.glide.request

import com.jormun.likeglide.glide.bean.Resources

/**
 * 给EngineJob通知上层资源已经加载完成的。
 * 在这个例子里就是Request监听Engine(实际上Engine又透传给了EngineJob)。
 */
interface ResourceCallback {
    fun onResourceReady(reference: Resources?)
}