package com.jormun.likehttp.chain

import com.jormun.likehttp.Response
import com.jormun.likehttp.net.HttpCodec
import java.io.IOException

/**
 * 请求拦截器
 * 真正发起请求的拦截器
 * 发起请求并且解析回来的数据，封装成response返回
 */
class CallServerInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val realChain = chain as RealInterceptorChain
        //取出链中保存连接器
        val connection = realChain.connection()
        //取出http信息解码器
        val httpCodec = realChain.httpCodec
        //连接器不为空，执行请求
        connection?.apply {
            //用连接器请求数据并且返回个Stream
            val callInputStream = call(httpCodec)
            //读取并解析第一行，比如 HTTP 200 OK 这样
            val readLine = httpCodec.readLine(callInputStream)
            //读取并解析头信息
            val headers = httpCodec.readHeaders(callInputStream)
            //看下是否需要设置长连接
            var isKeepAlive = false
            if (headers.containsKey(HttpCodec.HEAD_CONNECTION)) {
                isKeepAlive =
                    headers[HttpCodec.HEAD_CONNECTION].equals(HttpCodec.HEAD_VALUE_KEEP_ALIVE, true)
            }
            //看下是否有contentLength
            var contentLength = -1
            headers[HttpCodec.HEAD_CONTENT_LENGTH]?.apply {
                contentLength = toInt()
            }
            //看下是否分块
            var isChunked = false
            headers[HttpCodec.HEAD_TRANSFER_ENCODING]?.apply {
                isChunked = equals(HttpCodec.HEAD_VALUE_CHUNKED, true)
            }
            //如果contentLength大于0，则说明返回的response有body
            var body: String = ""
            if (contentLength > 0) {
                //解析并读取body
                body = String(httpCodec.readBytes(callInputStream, contentLength))
            } else if (isChunked) {
                body = httpCodec.readChunked(callInputStream)
            }
            //切掉第一行，取出响应码
            val split = readLine.split(" ")
            //更新一下连接时间
            connection.updateLastUseTime()
            //构建response并且返回，没有用到chain.proceed说明当前拦截器拦截结束，不再执行下个
            return Response(
                split[1].toInt(),
                realChain.request().method,
                realChain.request(),
                contentLength,
                headers,
                body,
                isKeepAlive
            )
        }
        throw IOException("connection is null.")
    }
}