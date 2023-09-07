package com.jormun.likehttp.net

import java.net.URLEncoder

/**
 * 请求body
 * 如果是post请求就需要这个了
 */
class RequestBody {

    private val CONTENT_TYPE = "application/x-www-form-urlencoded"
    private val CHARSET = "utf-8"

    val encodedBodys = HashMap<String, String>()

    fun contentType(): String {
        return CONTENT_TYPE
    }


    fun contentLength(): Int {
        return body().toByteArray().size
    }

    /**
     * 拼接body返回
     */
    fun body(): String {
        return buildString {
            for (encodedBody in encodedBodys) {
                append(encodedBody.key)
                append("=")
                append(encodedBody.value)
                append("&")
            }
            if (isNotEmpty()) {
                deleteCharAt(length - 1)
            }
        }
    }

    /**
     * 添加body
     * key-value形式添加
     */
    fun add(key: String, value: String): RequestBody {
        try {
            encodedBodys[URLEncoder.encode(key, CHARSET)] = URLEncoder.encode(value, CHARSET)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return this
    }


}