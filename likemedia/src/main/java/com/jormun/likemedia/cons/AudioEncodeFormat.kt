package com.jormun.likemedia.cons

import android.media.AudioFormat

object AudioEncodeFormat {
    //采样率
    const val SAMPLE_RATE_INHZ = 44100
    //通道数，MONO是双通道
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    //采样位数
    const val AUDIO_FORMAT_BIT = AudioFormat.ENCODING_PCM_16BIT

}