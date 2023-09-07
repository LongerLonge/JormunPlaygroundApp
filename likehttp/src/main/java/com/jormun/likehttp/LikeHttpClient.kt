package com.jormun.likehttp

import com.jormun.likehttp.chain.Interceptor
import com.jormun.likehttp.net.ConnectionPool
import com.jormun.likehttp.net.Request

/**
 * LikeHttp对外暴露的api类
 * 用建造者模式创建
 * 本身不参与实现具体功能，还是通过组合的方式对外提供功能给调用者使用。
 */
class LikeHttpClient(builder: Builder) : Call.Factory {

    constructor() : this(Builder())

    //连接池
    val connectionPool: ConnectionPool

    //重试次数
    private var retry: Int = 1

    //一堆拦截器
    val interceptors: List<Interceptor>

    //分发器
    val dispatcher: Dispatcher

    /**
     * 构造器模式创建LikeHttpClient
     */
    class Builder {
        var retry = 1

        val interceptors = mutableListOf<Interceptor>()

        val dispatcher = Dispatcher()

        val connectionPool = ConnectionPool()

        fun retry(retry: Int): Builder {
            this.retry = retry
            return this
        }

        fun addInterceptor(interceptor: Interceptor): Builder {
            interceptors.add(interceptor)
            return this
        }

    }

    init {
        connectionPool = builder.connectionPool
        retry = builder.retry
        interceptors = builder.interceptors
        dispatcher = builder.dispatcher
    }

    /**
     * 根据request创建call
     */
    override fun newCall(request: Request): Call {
        return RealCall(request, this)
    }


}