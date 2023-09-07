package com.jormun.likehttp

import com.jormun.likehttp.net.Request

class Response(
    val code: Int,
    val method:String,
   val request: Request,
    val contentLength: Int = -1,
    val headers: Map<String, String>,
    val body: String,
    val isKeepAlive: Boolean
) {


}