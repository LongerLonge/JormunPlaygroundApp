package com.jormun.likeglide.glide.bean

import android.graphics.Bitmap
import android.util.Log

/**
 *Bitmap的封装类，可以理解为就是实现了一些自定义行为的Bitmap。
 */
class Resources(var bitmap: Bitmap) {
    private val TAG = "Resources"

    //缓存的Bitmap，被封装的对象。


    //该资源的引用计数，也就是被引用的数量
    //如果为0，就代表需要从活动缓存回收到内存缓存中去
    private var acquired = 0

    private var resourceListener: ResourceListener? = null

    private var key: Key? = null//需要一个Key来方便查询

    /**
     * 类通信的接口，用来告诉外界自身的引用情况。
     * 假设资源需要从活动缓存中回收，也就是计数为0了，那么就回调这个接口。
     * 为什么要把bitmap回调回去呢，很简单，就是因为活动缓存和内存缓存是互相协助工作的，
     * 所以如果活动缓存已经不需要这个bitmap了，需要把这个bitmap抛出去让内存缓存回收。
     *
     */
    interface ResourceListener {
        fun onResourceReleased(key: Key, resources: Resources)
    }


    fun setResourceListener(key: Key, resourceListener: ResourceListener) {
        this.resourceListener = resourceListener
        this.key = key
    }

    //对bitmap进行释放，前提是引用计数不能大于0
    fun recycle() {
        if (acquired > 0) {
            return
        }
        /*bitmap?.apply {
            if (!isRecycled) {
                recycle()
            }
        }*/
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    //被取消引用一次移除就减去一个计数，如果为0就回调回收接口，把自己抛出去让其它类处理。
    fun release() {
        if (--acquired == 0) {
            resourceListener?.apply {
                key?.apply {
                    onResourceReleased(this, this@Resources)
                }
            }
        }
    }

    //用到了就把计数加一，前提是bitmap不能被回收，必须还存活。
    fun acquire() {
        /*bitmap?.apply {
            if (isRecycled) {
                Log.e(TAG, "acquire: bitmap is recycled.")
                return
            }
            ++acquired
        }*/
        if (bitmap.isRecycled) {
            Log.e(TAG, "acquire: bitmap is recycled.")
            return
        }
        ++acquired
    }

}