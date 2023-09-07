package com.jormun.likeglide.glide.cache

import android.content.ComponentCallbacks2
import android.util.LruCache
import java.util.NavigableMap
import java.util.TreeMap

/**
 * 数组复用池。
 * 解析图片的时候，把InputStream转换成Bitmap，需要用到数组。
 * 这里是把数组也做一个复用池出来，好让图片的转码解码过程更快速。
 * 同时图片的转码和解码也是一个耗费资源的操作，因为需要频繁操作数组，这里做个复用池也是为了提高性能。
 * @param maxSize 该复用池的最大可用长度。默认是4 * 1024 * 1024，也可以传个数进来作为大小。
 */
class LruArrayPool(private val maxSize: Int = ARRAY_POOL_SIZE_BYTES) : ArrayPool {
    companion object {
        const val ARRAY_POOL_SIZE_BYTES = 4 * 1024 * 1024
        private const val SINGLE_ARRAY_MAX_SIZE_DIVISOR = 2//单个资源的与maxsize 最大比例
        private const val MAX_OVER_SIZE_MULTIPLE = 8//溢出大小
    }

    //这个复用池实际上也是通过LruCache实现的
    private val cache: LruCache<Int, ByteArray>

    //NavigableMap包含一个ceilingKey(x)函数，这个函数可以查询出在该Map中大于或等于x的key
    private val sortedSizes: NavigableMap<Int, Int> = TreeMap()

    init {
        cache = object : LruCache<Int, ByteArray>(maxSize) {

            //LruCache需要重写下面两个核心函数
            override fun sizeOf(key: Int, value: ByteArray): Int {
                return value.size
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: Int,
                oldValue: ByteArray?,
                newValue: ByteArray?
            ) {
                if (oldValue != null)
                    sortedSizes.remove(oldValue.size)
            }
        }
    }

    /**
     * 从缓存中获取
     * @param len: 申请使用的数组长度
     */
    override fun get(len: Int): ByteArray {
        //获得等于或大于len的key
        val key = sortedSizes.ceilingKey(len)
        if (null != key) {
            //需要从缓存池取出的对象不能超过所需长度的8倍
            if (key <= MAX_OVER_SIZE_MULTIPLE * len) {
                val bytes = cache.remove(key)
                sortedSizes.remove(key)
                return bytes ?: ByteArray(len)
            }
        }
        return ByteArray(len)
    }

    /**
     * 存入缓存池中
     * @param data: 需要存放的数组，注意大小是有限制的
     */
    override fun put(data: ByteArray) {
        val length = data.size
        //太大了 不缓存
        if (!isSmallEnoughForReuse(length)) {
            return
        }
        //把长度也就是length作为Key存储，这样可以快速的利用ceilingKey(x)来获取
        sortedSizes[length] = 1//存放的情况：[Key: length]->[1]
        cache.put(length, data)
    }

    /**
     * 用来判断是否能加入缓存池的函数。
     * 这里的例子是不能超过最大缓存的一半。
     */
    private fun isSmallEnoughForReuse(byteSize: Int): Boolean {
        return byteSize <= maxSize / SINGLE_ARRAY_MAX_SIZE_DIVISOR
    }

    override fun clearMemory() {
        cache.evictAll()
    }

    override fun trimMemory(level: Int) {
        //内存不足时，要裁剪或者直接清空，看情况。
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
            || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        ) {
            cache.trimToSize(maxSize / 2)
        }
    }

    override fun getMaxSize(): Int {
        return maxSize
    }
}