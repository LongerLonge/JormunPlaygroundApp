package com.jormun.likemedia.utils

import android.content.Context
import com.jormun.likemedia.MyApplication

object UiUtils {



    fun px2dp(context: Context, px: Int): Int {
        val density = context.resources.displayMetrics.density
        return (px / density + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率从 dp(相对大小) 的单位 转成为 px(像素)
     */
    fun dpToPx(context: Context, dpValue: Float): Int {
        // 获取屏幕密度
        val scale = context.resources.displayMetrics.density
        // 结果+0.5是为了int取整时更接近
        return (dpValue * scale + 0.5f).toInt()
    }

    fun dpToPx(dpValue: Float): Int {
        // 获取屏幕密度
        val scale = MyApplication.appContext.resources.displayMetrics.density
        // 结果+0.5是为了int取整时更接近
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp(相对大小)
     */
    fun pxToDp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }


}