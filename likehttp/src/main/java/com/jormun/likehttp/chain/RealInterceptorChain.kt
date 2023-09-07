package com.jormun.likehttp.chain

import com.jormun.likehttp.Call
import com.jormun.likehttp.RealCall
import com.jormun.likehttp.Response
import com.jormun.likehttp.net.HttpCodec
import com.jormun.likehttp.net.HttpConnection
import com.jormun.likehttp.net.Request

/**
 * 链实现对象。
 * 封装了一堆链需要的环境参数信息对象等。
 * 链实际上只是笼统说法，这个类实际上只代表链上单个节点的信息。
 *
 * @param interceptors: 所有的拦截器
 * @param call: 需要执行的Call
 * @param index: 链当前的位置
 * @param request: 当前的request
 * @param httpConnection: 连接器
 */
class RealInterceptorChain(
    private val interceptors: List<Interceptor>,
    private val call: RealCall,
    private val index: Int,//链的当前位置，很关键
    private val request: Request,
    private var httpConnection: HttpConnection?
) : Interceptor.Chain {

    val httpCodec = HttpCodec()

    /**
     * 根据传入的实例快速构建一个新实例
     * 默认都是取传入实例的数据来构建
     * 也可以手动传入来改变某个数据，然后构建实例返回
     */
    internal fun copy(
        index: Int = this.index,
        interceptors: List<Interceptor> = this.interceptors,
        request: Request = this.request,
        call: RealCall = this.call,
        httpConnection: HttpConnection? = this.httpConnection
    ) = RealInterceptorChain(interceptors, call, index, request, httpConnection)

    override fun request(): Request {
        return request
    }

    override fun connection(): HttpConnection? {
        return httpConnection
    }

    override fun call(): Call {
        return call
    }


    override fun proceed(request: Request): Response {
        //根据节点下标，也就是链的当前位置，取出对应的拦截器
        val interceptor = interceptors[index]//在这里设计是一个链对应一个拦截器
        //构建链上下一个节点的对象
        val nextChain = copy(index = index + 1, request = request)
        //让拦截器执行
        return interceptor.intercept(nextChain)
    }
}