package com.jormun.likeglide.glide.bean

import java.security.MessageDigest


interface Key {
    //val STRING_CHARSET_NAME = "UTF-8"
    //val CHARSET = Charset.forName(STRING_CHARSET_NAME)

    /**
     * Adds all uniquely identifying information to the given digest.
     *
     *
     * Note - Using [java.security.MessageDigest.reset] inside of this method will result
     * in undefined behavior.
     */
    fun updateDiskCacheKey(messageDigest: MessageDigest)

    /**
     * For caching to work correctly, implementations *must* implement this method and [ ][.hashCode].
     */
    //override fun equals(o: Any?): Boolean

    /**
     * For caching to work correctly, implementations *must* implement this method and [ ][.equals].
     */
    //override fun hashCode(): Int

    fun getKeyBytes(): ByteArray
}