package com.jormun.likeglide.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ImageView
import kotlin.math.max

/**
 * 封装了ImageView的类。
 * 同时里面也会对ImageView进行测量，方便对图片进行处理。
 * 比如ImageView只有50*50，你原图1000*1000，肯定是没法直接显示。
 * 因此需要一个包装类来对ImageView的宽高来计算和获取。
 * 通常来说，计算ImageView的宽高的步骤执行在加载数据前。
 */
class ViewTarget(var view: ImageView) {
    companion object {
        var maxDisplayLength = -1
    }

    private var cb: SizeReadyCallback? = null
    private var layoutListener: LayoutListener? = null

    interface SizeReadyCallback {
        fun onSizeReady(width: Int, height: Int)
    }

    /**
     * 根据View的绘制原理，只有在一切准备就绪开始绘制的时候，才能获得真正的宽高。
     * view的宽高需要一个计算和赋值的过程，所以我们这里就用到了ViewTreeObserver。
     */
    class LayoutListener(private var viewTarget: ViewTarget?) : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            viewTarget?.apply {
                checkCurrentDimens()
            }
            return true
        }

        fun release() {
            viewTarget = null
        }
    }

    /**
     * 获取ImageView的宽高信息
     */
    private fun checkCurrentDimens() {
        cb?.apply {
            val currentWidth: Int = getTargetWidth()
            val currentHeight: Int = getTargetHeight()
            if (currentHeight <= 0 && currentWidth <= 0) {
                return
            }
            onSizeReady(currentWidth, currentHeight)
            cancel()
        }

    }

    /**
     * 获取ImageView的高度
     */
    private fun getTargetHeight(): Int {
        //减去上下两边的padding才是真正的高度
        val verticalPadding = view.paddingTop + view.paddingBottom
        //获得其在父布局中的属性 1、给定的大小 2、wrap_content
        val layoutParams = view.layoutParams
        val layoutParamSize = layoutParams?.height ?: 0
        return getTargetDimen(view.height, layoutParamSize, verticalPadding)
    }

    /**
     * 获取ImageView的宽度
     */
    private fun getTargetWidth(): Int {
        //获得view的padding
        val horizontalPadding = view.paddingLeft + view.paddingRight
        //获得view的布局属性 1、给定的大小 2、wrap_content
        val layoutParams = view.layoutParams
        val layoutParamSize = layoutParams?.width ?: 0
        return getTargetDimen(view.width, layoutParamSize, horizontalPadding)
    }

    /**
     * 计算ImageView的宽高
     * @param viewSize 通过view.getWidth或者view.getHeight获得的宽或者高
     * @param paramSize 通过view.layoutParams获取的宽或者高
     * @param paddingSize 是否有左右或者上下的padding
     */
    private fun getTargetDimen(viewSize: Int, paramSize: Int, paddingSize: Int): Int {
        //1、如果是固定大小，也就是说直接就能通过layoutParams获取到宽高
        //能直接从layoutParams获取到宽高就代表直接在xml中写好了，width=100dp这样
        //如果写的是 width="match_parent"，就会返回-1
        val adjustedParamSize = paramSize - paddingSize
        if (adjustedParamSize > 0) {
            return adjustedParamSize
        }

        //2、如果能够由 view.getWidth() 获得大小
        //能从view.getWidth()获取大小代表是写的match_parent之类的，让父容器去计算并且赋予view宽高
        val adjustedViewSize = viewSize - paddingSize
        if (adjustedViewSize > 0) {
            return adjustedViewSize
        }

        //3、如果布局属性设置的是包裹内容并且我们不能接到回调了
        // 回调 是什么？ addOnPreDrawListener
        //表示不会回调 onPreDraw
        //兜底策略
        return if (!view.isLayoutRequested && paramSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
            getMaxDisplayLength(view.context)
        } else 0
    }

    /**
     * 获得一个最大允许的view大小
     * @param context
     * @return
     */
    private fun getMaxDisplayLength(context: Context): Int {
        if (maxDisplayLength == -1) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val displayDimensions = Point()
            //获得屏幕大小
            display.getSize(displayDimensions)
            // 最大的屏幕大小
            maxDisplayLength = max(displayDimensions.x, displayDimensions.y)
        }
        return maxDisplayLength
    }

    /**
     * 把监听从ViewTreeObserver移除
     */
    fun cancel() {
        val observer = view.viewTreeObserver
        if (observer.isAlive) {
            observer.removeOnPreDrawListener(layoutListener)
        }
        layoutListener?.release()
        layoutListener = null
        cb = null
    }

    fun onLoadFailed(error: Drawable?) {
        view.setImageDrawable(error)
    }

    fun onLoadStarted(placeholderDrawable: Drawable?) {
        view.setImageDrawable(placeholderDrawable)
    }

    /**
     * 获得ImageView的宽高
     * @param cb: 获取成功后需要的回调
     */
    fun getSize(cb: SizeReadyCallback) {
        //获得宽
        val currentWidth = getTargetWidth()
        //获得高
        val currentHeight = getTargetHeight()
        //如果能正常获取，就直接返回
        if (currentHeight > 0 && currentWidth > 0) {
            cb.onSizeReady(currentWidth, currentHeight)
            return
        }
        //如果没有，可能是界面还没初始化完，就等OnPreDraw的时候去获取
        this.cb = cb
        if (layoutListener == null) {
            //视图绘制前回调 回调中能获得宽与高
            val observer = view.viewTreeObserver
            layoutListener = LayoutListener(this)
            observer.addOnPreDrawListener(layoutListener)
        }
    }

    /**
     * 数据层成功加载完Bitmap了，交给这里set入去。
     */
    fun onResourceReady(bitmap: Bitmap?) {
        view.setImageBitmap(bitmap)
    }

}