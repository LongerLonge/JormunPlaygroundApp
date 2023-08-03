package com.jormun.likeglide.glide.recycle

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import java.util.NavigableMap
import java.util.TreeMap

/**
 * 通过Lru的方式来实现复用池
 */
class LruBitmapPool(maxSize: Int) : LruCache<Int, Bitmap>(maxSize), BitmapPool {
    private val TAG = "LruBitmapPool"
    private val MAX_OVER_SIZE = 2//不能超过大小，这里我们定2倍

    private val map: NavigableMap<Int, Bitmap> = TreeMap()//NavigableMap可以查询出最接近的值，下面有应用。

    private var isRemoved = false//标记是否主动移除

    override fun sizeOf(key: Int?, value: Bitmap?): Int {
        return value?.allocationByteCount ?: 1
    }


    override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
        super.entryRemoved(evicted, key, oldValue, newValue)
        //如果是被动移除(也就是超出大小被Lru抛弃)，那就让其直接释放。
        if (!isRemoved) oldValue?.recycle()
    }

    /**
     * 存放Bitmap到复用池
     */
    override fun put(bitmap: Bitmap?) {
        if (bitmap == null) {
            Log.e(TAG, "put: ")
            return
        }
        //不可复用，直接释放
        if (!bitmap.isMutable) {
            bitmap.recycle()
            return
        }
        //超过最大大小，直接释放
        val size = bitmap.allocationByteCount
        if (size > maxSize()) {
            bitmap.recycle()
            return
        }
        //都满足，就存
        put(size, bitmap)

    }

    /**
     * 从复用池中获取Bitmap
     */
    override fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        //算出Bitmap的大小，长乘以宽乘以编码格式，如果是8888那就4字节，565就2字节(8位为一字节)
        val bSize = width * height * (if (config == Bitmap.Config.ARGB_8888) 4 else 2)
        //获取大于或者等于某个值的Key(这里我们存放的Key是size)
        val bKey = map.ceilingKey(bSize)
        //如果Key(也就是size)大于等于我们定义的最大值，我们就可以让其返回复用，否则就不能复用直接新建吧。
        if (bKey != null && bKey <= bSize * MAX_OVER_SIZE) {
            isRemoved = true//是我们主动移除，要标记。
            return remove(bKey)//注意是remove，让Lru抛出去。
        }
        return null
    }

    override fun trimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            trimToSize(maxSize() / 2)
        }
    }

    override fun clearMemory() {
        evictAll()
    }
}