package com.jormun.likeglide.glide.load

import com.jormun.likeglide.glide.bean.Key
import java.security.MessageDigest

/**
 * 给Engine用的Key
 * 用来唯一定位某个资源对象。
 * 注意只作用在Engine层面，底层缓存不是用这个Key。
 */
class EngineKey(private val model: Any, private val width: Int, private val height: Int) : Key {


    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(getKeyBytes())
    }

    override fun getKeyBytes(): ByteArray {
        return toString().toByteArray()
    }

    /**
     * equals
     * hashCode
     * toString
     * 都是AS自动生成的
     * 快捷键是 alt+insert
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EngineKey

        if (model != other.model) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = model.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }

    override fun toString(): String {
        return "EngineKey(model=$model, width=$width, height=$height)"
    }


}