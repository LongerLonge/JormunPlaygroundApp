package com.jormun.likehttp.net

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

class HttpCodec {

    /**
     * 解析需要用到的一堆常量
     */
    companion object {
        //回车和换行
        val CRLF = "\r\n"

        //回车
        val CRCR = "\n"

        //回车的int值
        val CR = 13

        //换行的int值
        val LF = 10

        val SPACE = " "
        val VERSION = "HTTP/1.1"
        val COLON = ":"

        //拼接或者获取map的值用的
        val HEAD_HOST = "Host"
        val HEAD_CONNECTION = "Connection"
        val HEAD_CONTENT_TYPE = "Content-Type"
        val HEAD_CONTENT_LENGTH = "Content-Length"
        val HEAD_TRANSFER_ENCODING = "Transfer-Encoding"

        val HEAD_VALUE_KEEP_ALIVE = "keep-alive"
        val HEAD_VALUE_CHUNKED = "chunked"
    }

    //申请足够大的内存记录读取的数据 (一行)
    private val byteBuffer: ByteBuffer = ByteBuffer.allocate(10 * 1024)

    init {

    }

    /**
     * 把请求信息解析，并且写入到输出流里面
     * 写入的是socket的输出流，写完就代表发完请求了
     * @param os: socket打开连接的输出流
     * @param request:请求信息
     */
    @Throws(IOException::class)
    fun writeRequest(os: OutputStream, request: Request) {
        val requestString = buildString {

            //拼接请求行： GET / HTTP/1.1
            append(request.method)
            append(SPACE)
            append(request.url.file)
            append(SPACE)
            append(VERSION)
            append(CRCR)//需要拼接一下/r，不然报错

            //拼接请求头, 如
            //Connection: keep-alive
            //Host: www.baidu.com
            //User-Agent: Mozilla/5.0
            for (header in request.headers) {
                append(header.key)
                append(COLON)
                append(SPACE)
                append(header.value)
                append(CRLF)
            }
            append(CRCR)//需要拼接一下/r，不然报错
            //拼接请求体，如果有的话
            request.body?.apply {
                append(body())
            }
        }
        //写出到OutputStream(实际上就是通过socket写出到网络)
        os.write(requestString.toByteArray())
        os.flush()
    }

    /**
     * 一行行读取
     */
    @Throws(IOException::class)
    fun readLine(inputStream: InputStream): String {
        try {
            var readByte: Byte
            var readInt = 0
            var isMaybeEofLine = false
            //标记初始位置
            byteBuffer.clear()
            byteBuffer.mark()

            while (readInt != -1) {
                //一个个字节读
                readInt = inputStream.read()
                readByte = readInt.toByte()
                //放到buffer缓存里
                byteBuffer.put(readByte)
                // 读取到/r则记录，判断下一个字节是否为/n
                if (readInt == CR) {
                    isMaybeEofLine = true
                } else if (isMaybeEofLine) {
                    //上一个字节是/r 并且本次读取到/n
                    if (readInt == LF) {
                        //下面是为了把目前读取的所有字节构建成String返回

                        //根据已经读到的位置构建一个长度一致的字节数组
                        val lineBytes = ByteArray(byteBuffer.position())
                        //返回标记位置
                        byteBuffer.reset()
                        //把已读到数据全部填充到新的字节数组里
                        byteBuffer[lineBytes]
                        //清空所有index并重新标记
                        byteBuffer.clear()
                        byteBuffer.mark()
                        //构建String并返回
                        return String(lineBytes)
                    }
                    isMaybeEofLine = false
                }
            }
        } catch (e: IOException) {
            throw IOException(e)
        }
        throw IOException("readLine fail.")
    }

    /**
     * 解析头部信息
     */
    @Throws(IOException::class)
    fun readHeaders(inputStream: InputStream): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        while (true) {
            //读取一行
            val readLine = readLine(inputStream)
            //读到空行，则下面的为body，不读了
            if (isEmptyLine(readLine)) {
                break
            }
            //是header部分就开始解析
            val index = readLine.indexOf(":")
            if (index > 0) {
                val key = readLine.substring(0, index)
                // ": "移动两位到 总长度减去两个("\r\n")
                val value: String = readLine.substring(index + 2, readLine.length - 2)
                headers[key] = value
            }
        }
        return headers
    }

    /**
     * 是否为回车+换行
     */
    private fun isEmptyLine(line: String): Boolean {
        return line == "\r\n"
    }

    /**
     * 按照一定长度读取流中数据
     * @param len: 需要读取多长数据
     * @return 返回一个填充好的字节数组
     */
    @Throws(IOException::class)
    fun readBytes(inputStream: InputStream, len: Int): ByteArray {
        val byteArray = ByteArray(len)
        var readInt = 0
        while (true) {
            readInt += inputStream.read(byteArray, readInt, len - readInt)
            if (readInt == len) {
                return byteArray
            }
        }
    }

    /**
     * 按块读取
     */
    @Throws(IOException::class)
    fun readChunked(inputStream: InputStream): String {
        var len = -1
        var isEmptyData = false
        buildString {
            while (true) {
                if (len < 0) {
                    var line = readLine(inputStream)
                    line = line.substring(0, line.length - 2)
                    len = Integer.valueOf(line, 16)
                    isEmptyData = len == 0
                } else {
                    val readBytes = readBytes(inputStream, len + 2)
                    append(String(readBytes))
                    len = -1
                    if (isEmptyData) {
                        return toString()
                    }
                }
            }
        }
    }

    /*    @Throws(IOException::class)
        fun readChunked(inputStream: InputStream): String {
            var len = -1
            var isEmptyData = false
            val chunked = StringBuffer()
            while (true) {
                //解析下一个chunk长度
                if (len < 0) {
                    var line = readLine(inputStream)
                    line = line.substring(0, line.length - 2)
                    len = Integer.valueOf(line, 16)
                    //chunk编码的数据最后一段为 0\r\n\r\n
                    isEmptyData = len == 0
                } else {
                    //块长度不包括\r\n  所以+2将 \r\n 读走
                    val bytes = readBytes(inputStream, len + 2)
                    chunked.append(bytes.toString())
                    len = -1
                    if (isEmptyData) {
                        return chunked.toString()
                    }
                }
            }
        }*/
}