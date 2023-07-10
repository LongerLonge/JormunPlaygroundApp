package com.jormun.likeglide.glide.recycle

import android.graphics.Bitmap

/**
 * 复用池的顶层接口，用来描述复用池的统一行为
 */
interface BitmapPool {

    fun put(bitmap: Bitmap?)

    fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap?
}