package com.jormun.likemedia.net

interface SocketCallback {
    fun callback(data: ByteArray)
}