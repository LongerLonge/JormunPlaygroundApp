package com.jormun.likemedia.codec

interface BaseEncoder {
    fun startEncoder()

    fun dealSpsFrameAndSave(frameBuffer: ByteArray)
}