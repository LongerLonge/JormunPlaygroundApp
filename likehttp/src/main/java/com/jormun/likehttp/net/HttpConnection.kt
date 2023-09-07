package com.jormun.likehttp.net

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

/**
 * 连接器
 * 用Socket实现的
 */
class HttpConnection {


    val HTTPS = "https"

    private lateinit var socket: Socket//连接肯定要用Socket了
    private lateinit var inputStream: InputStream//Socket打开后的写出流(返回值)
    private lateinit var outputStream: OutputStream//Socket打开后的写入流(请求值)
    lateinit var request: Request//request
    var lastUsetime = 0L//上一次发起请求的时间
        private set
    //判断Socket是否被创建和初始化
    private var isSocketReady: Boolean = (::socket.isInitialized)

    /**
     * 简单判断连接器是否可以复用
     * @param host: 主机
     * @param port: 端口
     */
    fun isSameAddress(host: String, port: Int): Boolean {
        if (!isSocketReady) {
            return false
        }
        //判断标准很简单，就是看传入的host和port是否与当前socket一致而已
        return socket.inetAddress.hostName == host && port == socket.port
    }

    /**
     * 关闭连接，就是关闭socket
     */
    fun closeQuietly() {
        if (!isSocketReady) {
            try {
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 发起请求
     * @param httpCodec: 信息解码器，根据传入的解码器处理请求和返回数据
     * @return: 输出流，也就是返回值的Stream
     */
    @Throws(IOException::class)
    fun call(httpCodec: HttpCodec): InputStream {
        try {
            //socket初始化
            createSocket()
            //写入请求数据
            httpCodec.writeRequest(outputStream, request)
            //返回输出流，也就是返回值的Stream给出去外面自己读
            return inputStream
        } catch (e: IOException) {
            closeQuietly()
            throw IOException(e)
        }
    }

    /**
     * 更新连接器的上一次请求时间
     */
    fun updateLastUseTime() {
        lastUsetime = System.currentTimeMillis()
    }

    /**
     * 创建Socket并且打开连接
     */
    @Throws(IOException::class)
    private fun createSocket() {
        if (!isSocketReady || socket.isClosed) {
            val url = request.url
            //创建Socket
            socket = if (url.protocol.equals(HTTPS, true)) {
                SSLSocketFactory.getDefault().createSocket()
                //SSLContext.getDefault().socketFactory.createSocket()
            } else {
                Socket()
            }
            //打开连接
            socket.connect(InetSocketAddress(url.host, url.port))
            //获取写入写出流
            outputStream = socket.getOutputStream()
            inputStream = socket.getInputStream()
        }
    }


}