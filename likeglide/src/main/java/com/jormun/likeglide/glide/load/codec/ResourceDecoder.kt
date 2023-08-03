package com.jormun.likeglide.glide.load.codec

import android.graphics.Bitmap
import java.io.IOException

/**
 * Bitmap解码器抽象行为接口。
 * @param T 输入的数据源类型，比如InputStream
 */
interface ResourceDecoder<T> {

    /**
     * 过滤，是否处理这个数据源
     */
    @Throws(IOException::class)
    fun handles(source: T): Boolean

    /**
     * 把数据源转换成Bitmap
     * 泛型T是输入的数据类型，比如InputStream
     * 实现类见：
     * @see StreamBitmapDecoder
     */
    @Throws(IOException::class)
    fun decode(source: T, width: Int, height: Int): Bitmap?
}