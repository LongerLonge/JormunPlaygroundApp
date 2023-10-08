package com.jormun.likemedia.cons

import android.media.MediaFormat
import com.jormun.likemedia.codec.CodeType

/**
 * 编解码配置同一集中处。
 */
object VideoEncodeFormat {
    //编解码格式的宽高
    const val VIDEO_WIDTH = 1080
    const val VIDEO_HEIGHT = 1920

    /*val VIDEO_WIDTH = dpToPx(393f)
    val VIDEO_HEIGHT = dpToPx(699f)*/
    /*val VIDEO_WIDTH = 1440
    val VIDEO_HEIGHT = 2560*/

    //编解码格式的类型，比如avc，hevc等。
    const val VIDEO_MIMETYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    //val VIDEO_MIMETYPE = MediaFormat.MIMETYPE_VIDEO_HEVC

    val CODE_TYPE = CodeType.H264
    //val CODE_TYPE = CodeType.H265

    const val FRAME_RATE = 20

    const val I_FRAME_INTERVAL = 1

    const val BIT_RATE = VIDEO_WIDTH * VIDEO_HEIGHT
}