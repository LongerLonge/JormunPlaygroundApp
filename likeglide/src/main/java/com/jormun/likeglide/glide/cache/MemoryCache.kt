package com.jormun.likeglide.glide.cache

import com.jormun.likeglide.glide.bean.Key
import com.jormun.likeglide.glide.bean.Resources

//内存缓存，抽象化的表达，这里面定义了内存缓存的行为
interface MemoryCache{

    interface ResourceRemoveListener {
        fun onResourceRemove(resources: Resources)
    }

    fun setResourceRemoveListener(resourceRemoveListener: ResourceRemoveListener)

    fun put(key: Key, resources: Resources): Resources

    fun get(key: Key): Resources

    fun removeResource(key: Key): Resources
}