package com.jormun.likehttp.net

import java.net.URL

class HttpUrl(url: String) {
    val protocol: String//协议，比如https
    val host: String//主机域名，比如baidu或者192.xxx
    var file: String//文件路径，也就是主机域名后面的路径
    var port: Int//端口
    val scheme: String//

    init {
        val url1 = URL(url)
        host = url1.host
        file = url1.file
        file = file.ifEmpty { "/" }
        protocol = url1.protocol
        port = url1.port
        port = if (port == -1) url1.defaultPort else port
        scheme = parseScheme(url)
    }

    private fun parseScheme(url: String): String {
        return if (url.startsWith("https")) "https:" else "http:"
    }

    val isHttps: Boolean = scheme == "https"
}