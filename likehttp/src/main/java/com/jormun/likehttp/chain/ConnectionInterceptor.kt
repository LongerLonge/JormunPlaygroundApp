package com.jormun.likehttp.chain

import com.jormun.likehttp.RealCall
import com.jormun.likehttp.Response
import com.jormun.likehttp.net.HttpConnection

/**
 * 连接拦截器
 * 连接拦截器主要是查找连接池是否还有可复用的连接
 * 因为连接每次创建和通信，都需要耗费资源和时间
 * 这里通过查找缓存的方式来优化性能
 * 当然没有的话就弄个新的。
 * 实际上okhttp对是否能复用连接有诸多限制，这里就跳过只是以最简单的方式实现一下。
 */
class ConnectionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        //取出call
        val realCall = chain.call() as RealCall
        //取出request
        val request = chain.request()
        //从call里取出client
        val likeHttpClient = realCall.likeHttpClient
        //去连接池找一下有没有可以复用的，这里只是简单通过host和port判断而已
        var con = likeHttpClient.connectionPool.get(request.url.host, request.url.port)
        //没有可以复用的就创建一个
        if (con == null) {
            con = HttpConnection()
        }
        //给连接器赋值一下request，不然不知道请求信息
        con.request = request
        //根据当前的链信息，手动构建下一个链并且设置连接器
        val realInterceptorChain = chain as RealInterceptorChain
        val copyChain = realInterceptorChain.copy(httpConnection = con)
        //手动让下个链的拦截器启动获取数据
        val response = copyChain.proceed(request)
        //如果是长连接那就缓存这个连接器
        if (response.isKeepAlive) {
            likeHttpClient.connectionPool.put(con)
        }
        //因为这里返回了具体值，而没用到下个链的proceed方法，等于是拦截成功并返回了。
        //只不过这个值是我们自己手动创建一个链节点并且从它里面获取数据而已。
        return response
    }
}