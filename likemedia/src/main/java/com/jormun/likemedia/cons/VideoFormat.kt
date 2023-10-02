package com.jormun.likemedia.cons

import android.media.MediaFormat
import com.jormun.likemedia.codec.CodeType

/**
 * 编解码配置同一集中处。
 */
object VideoFormat {
    //编解码格式的宽高
    val VIDEO_WIDTH = 1080
    val VIDEO_HEIGHT = 1920

    /*val VIDEO_WIDTH = dpToPx(393f)
    val VIDEO_HEIGHT = dpToPx(699f)*/
    /*val VIDEO_WIDTH = 1440
    val VIDEO_HEIGHT = 2560*/

    //编解码格式的类型，比如avc，hevc等。
    val VIDEO_MIMETYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    //val VIDEO_MIMETYPE = MediaFormat.MIMETYPE_VIDEO_HEVC

    val CODE_TYPE = CodeType.H264
    //val CODE_TYPE = CodeType.H265
}