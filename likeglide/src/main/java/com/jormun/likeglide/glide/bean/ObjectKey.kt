package com.jormun.likeglide.glide.bean

import com.jormun.likeglide.glide.load.model.HttpUriLoader
import com.jormun.likeglide.glide.load.model.ModelLoaderRegistry
import java.security.MessageDigest

/**
 * Key接口的实现类，任何一个Object都可以传进来获得一个唯一Key。
 */
class ObjectKey(private val oKey: Any) : Key {


    //关键的方法，对Byte进行Sha加密
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(getKeyBytes())
    }

    override fun getKeyBytes(): ByteArray {
        return oKey.toString().toByteArray()
    }

    //下面这两个方法直接快捷键alt+insert然后选 equal()&hashcode() 就可以生成
    //生成这两个的目的是为了让Map能够识别重复的数据防止冲突和多余数据保存。
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ObjectKey

        if (oKey != other.oKey) return false

        return true
    }

    override fun hashCode(): Int {
        return oKey.hashCode()
    }


}