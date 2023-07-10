package com.jormun.likeglide.glide.cache

import android.util.LruCache
import com.jormun.likeglide.glide.bean.Key
import com.jormun.likeglide.glide.bean.Resources

/**
 * 内存缓存的实现类，采用的是Lru算法
 *构造方法需要传一个容器大小也就是maxsize
 */
class LruMemoryCache(maxSize: Int) : MemoryCache, LruCache<Key, Resources>(maxSize) {

    private var resourceRemoveListener: MemoryCache.ResourceRemoveListener? = null
    private var isDoRemoved = false//标记是否主动移除而非被动

    override fun setResourceRemoveListener(resourceRemoveListener: MemoryCache.ResourceRemoveListener) {
        this.resourceRemoveListener = resourceRemoveListener
    }

    /**
     * 主动移除某个对象，然后我们可以自己做一些业务逻辑。
     */
    override fun removeResource(key: Key): Resources {
        isDoRemoved = true//是主动移除，做标记
        val remove = remove(key)
        return remove
    }

    /**
     *关键函数sizeOf，需要重写。
     *重写这个的目的是获取需要使用的Bitmap大小
     */
    override fun sizeOf(key: Key, value: Resources): Int {
        value.bitmap?.apply {
            /**
             *4.4以上使用这个api获取完整的Bitmap大小(被复用的旧Bitmap)
             *4.4以下用byteCount
             *为什么这样做，Bitmap被复用过后byteCount和allocationByteCount会变得不一样
             * 前者是最新的Bitmap大小，而后者是完整的Bitmap大小，我们要的是后者。
             */
            return allocationByteCount
        }
        return 0
    }

    /**
     *关键函数entryRemoved，需要重写。
     *重写这个的目的是在移除Bitmap的时候抛出去让其它地方处理。
     *该方法由父类LruCache调用，这个函数只有两种情况下才会被调用：
     * 一个是put的时候超出大小会抛出尾部，第二个就是put完后发现还是超了大小就会继续抛出直到符合长度为止。
     */
    override fun entryRemoved(
        evicted: Boolean,
        key: Key,
        oldValue: Resources?,//旧值，也就是被抛出的对象
        newValue: Resources//新值，也就是被新塞进去的对象
    ) {
        /**
         *  被抛出分两种情况，主动和被动，主动是我们自己调用remove，被动就是上面说的两种情况。
         * 我们这里只监听被动的情况，主动抛出我们自己处理。
         */
        if (oldValue != null && !isDoRemoved)
            resourceRemoveListener?.onResourceRemove(oldValue)
    }
}