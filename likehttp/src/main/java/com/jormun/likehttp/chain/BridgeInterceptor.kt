package com.jormun.likehttp.chain

import com.jormun.likehttp.Response
import java.net.CookieManager

/**
 * 桥接拦截器, 又可称为头部拦截器
 * 应用层的数据并不能直接用在连接层(反过来也是)
 * 因此需要一个桥接拦截器来把数据进行一个处理
 * 而桥接拦截器(头拦截器)是处理请求头和响应头的拦截器
 * 比如给请求加请求头，解析回来后的响应头
 * 正如其名，桥接了应用层和连接层
 */
class BridgeInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val userRequest = chain.request()
        val newBuilder = userRequest.newBuilder()
        //说明有body，可能是post请求，需要设置头部参数
        val body = userRequest.body

        //若body不为空
        if (body != null) {
            //第一步、设置content-type
            newBuilder.addHeader("Content-Type", body.contentType())
            //第二步、设置长度
            val contentLength = body.contentLength()
            if (contentLength == -1) {//-1说明需要分段解析，填下面那个参数即可
                newBuilder.addHeader("Transfer-Encoding", "chunked")
            } else
                newBuilder.addHeader("Content-Length", contentLength.toString())
        }

        //下面这些就是公用的参数，get put都需要

        //host
        if (userRequest.headers["Host"] == null) {
            newBuilder.addHeader("Host", userRequest.url.host)
        }

        //Connection
        if (userRequest.headers["Connection"] == null) {
            newBuilder.addHeader("Connection", "keep-alive")
        }

        //Accept-Encoding:gzip, 是否接受压缩数据，压缩的数据是body，如果给了压缩标记位返回值也要解压
        /*if (userRequest.headers["Accept-Encoding"] == null && userRequest.headers["Range"] == null) {
            newBuilder.addHeader("Accept-Encoding","gzip")
        }*/

        //Cookie okhttp实现比较复杂这里略过
        //newBuilder.addHeader("Cookie","aaaaaa")


        //User-Agent
        if (userRequest.headers["User-Agent"] == null) {
            newBuilder.addHeader("User-Agent", "Mozilla/5.0")
        }

        //继续往下丢，丢给下个链的拦截器处理，换句话说这里不进行拦截
        val networkResponse = chain.proceed(newBuilder.build())

        //假设有压缩或者Cookie的，需要处理一下然后再构建一个response返回
        //比如gzip，需要对body进行一下解压才能返回
        //这里简化就直接略过返回了

        return networkResponse
    }
}