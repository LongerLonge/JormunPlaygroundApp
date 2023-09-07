package com.jormun.likehttp.chain

import com.jormun.likehttp.Response
import com.jormun.likehttp.net.HttpUrl
import com.jormun.likehttp.net.Request
import com.jormun.likehttp.state.State.HTTP_PERM_REDIRECT
import com.jormun.likehttp.state.State.HTTP_TEMP_REDIRECT
import java.io.IOException
import java.net.HttpURLConnection.HTTP_MULT_CHOICE
import java.net.HttpURLConnection.HTTP_MOVED_PERM
import java.net.HttpURLConnection.HTTP_MOVED_TEMP
import java.net.HttpURLConnection.HTTP_SEE_OTHER

/**
 * 重试拦截器
 */
class RetryInterceptor : Interceptor {

    companion object {
        /**
         * How many redirects and auth challenges should we attempt? Chrome follows 21 redirects; Firefox,
         * curl, and wget follow 20; Safari follows 16; and HTTP/1.0 recommends 5.
         */
        private const val MAX_FOLLOW_UPS = 20
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val call = chain.call()
        //发起请求的request类，重定向的话这个会被改掉
        var request: Request = chain.request()
        //如果需要重定向或者重新请求，这个就是上一次请求的response
        var priorResponse: Response
        //重试次数，默认给20
        var retryCount = 0
        //错误
        var exception: IOException? = null

        while (true) {
            try {
                if (call.isCanceled()) {
                    throw IOException("This call is Canceled!")
                }
                try {
                    //往下丢，丢给下个链的拦截器去处理，换句话说这里不进行拦截
                    val response = chain.proceed(request)
                    //检查是否需要重定向，不需要就直接返回response
                    val checkRequest = checkIsNeedRedirect(response) ?: return response

                    //不为空说明需要重定向
                    //赋值给request，直接通过while循环进入下一轮请求
                    request = checkRequest
                    priorResponse = response

                    //超过重试次数，退出循环
                    if (++retryCount > MAX_FOLLOW_UPS) {
                        if (exception == null) exception = IOException("Too many retry.")
                        throw exception
                    }

                } catch (e: IOException) {
                    //请求有异常，那就记录一下异常然后继续循环
                    exception = e
                    continue
                }

            } finally {
                //cancel
                /*if (exception != null) {
                    throw exception
                }*/
            }
        }
    }

    /**
     * 检查是否需要重定向
     */
    private fun checkIsNeedRedirect(response: Response): Request? {
        //code是300-309的，都需要重定向
        when (response.code) {
            HTTP_PERM_REDIRECT, HTTP_TEMP_REDIRECT, HTTP_MULT_CHOICE, HTTP_MOVED_PERM, HTTP_MOVED_TEMP, HTTP_SEE_OTHER -> {
                return buildRedirectRequest(response)
            }
        }
        return null
    }

    /**
     * 如果需要重定向则需要重新构建请求
     */
    private fun buildRedirectRequest(userResponse: Response): Request? {
        //重定向的url藏在返回头的Location里面
        val location = userResponse.headers["Location"] ?: return null
        val builder = Request.Builder().setUrl(location)
        if (userResponse.request.method == "GET") builder.get() else builder.post(userResponse.request.body)
        return builder.build()
    }
}