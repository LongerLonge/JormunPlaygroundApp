package com.jormun.likehttp

/**
 * 回调接口，用来通信
 */
interface Callback {

    /**
     * 成功
     */
    fun onResponse(call: Call, response: Response)

    /**
     * 失败
     */
    fun onFailure(call: Call, throwable: Throwable)
}