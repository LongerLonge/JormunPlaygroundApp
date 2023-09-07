package com.jormun.likehttp

import com.jormun.likehttp.net.Request
import java.io.IOException
import kotlin.jvm.Throws

/**
 * Call的行为抽象描述类。
 * 由这个类提供call行为等
 */
interface Call {

    /**
     *获取request
     */
    fun request(): Request

    /**
     * 同步阻塞式请求
     * 会抛出IOException
     */
    @Throws(IOException::class)
    fun execute(): Response

    /**
     * 异步请求
     */
    fun enqueue(callback: Callback)


    /**
     * 是否关闭
     */
    fun isCanceled(): Boolean


    /**
     * 关闭请求
     */
    fun cancel()

    /**
     * 工厂，创建Call用的
     * 在这里一并描述了
     */
    interface Factory {
        /**
         * 根据request创建Call
         */
        fun newCall(request: Request): Call
    }
}