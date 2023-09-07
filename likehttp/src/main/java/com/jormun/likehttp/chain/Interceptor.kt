package com.jormun.likehttp.chain

import com.jormun.likehttp.Call
import com.jormun.likehttp.Response
import com.jormun.likehttp.net.HttpConnection
import com.jormun.likehttp.net.Request
import java.io.IOException
import kotlin.jvm.Throws

/**
 * 拦截器抽象行为描述接口
 */
interface Interceptor {
    /**、
     * 关键方法，又称为拦截方法。
     * 这里传入的Chain实际上是下个节点的Chain，并不是当前节点的。
     * 所以需要丢给下个节点处理的话，就用这个Chain，不需要就在当前方法返回
     * 所以才叫拦截方法
     * @param chain: 下一个Chain对象
     */
    @Throws(IOException::class)
    fun intercept(chain: Chain): Response

    /**
     * 责任链模式的链描述接口
     * 实际上这个链代表的只是链的节点信息
     * 并不是代表整个链
     * 同时这个链里面包含了当前整个链都可能需要的环境信息和参数对象等。
     */
    interface Chain {

        /**
         * request，链里面各个节点都可能需要用到
         */
        fun request(): Request

        /**
         * 连接器，链里面有节点可能需要
         */
        fun connection(): HttpConnection?

        /**
         * 获取call，链里面可能有节点需要
         */
        fun call(): Call

        /**
         * 关键方法
         * 接收request，然后生成下个Chain给Interceptor用
         *
         * @param request: 当前链的request，或者是上个链传给你的。
         */
        @Throws(IOException::class)
        fun proceed(request: Request): Response

    }
}