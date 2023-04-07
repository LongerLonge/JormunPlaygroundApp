package com.jormun.retrofit

import okhttp3.Callback
import java.io.IOException

//用来隔离调用层和Retrofit的中间接口，这样模块对外只提供“行为”而不是对象的实例
interface RfCall {

    @Throws(IOException::class)//对外提供同步请求的行为。
    fun execute(): String

    fun enqueue(callback: Callback)//对外提供异步请求的行为，需要调用方自己提供回调来通知。

    suspend fun enqueue(): String//协程异步请求的行为
}