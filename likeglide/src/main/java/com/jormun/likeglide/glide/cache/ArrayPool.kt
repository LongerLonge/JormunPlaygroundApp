package com.jormun.likeglide.glide.cache

/**
 * 数组复用池的顶层行为接口。
 * 描述了数组复用池的统一行为。
 */
interface ArrayPool {
    /**
     * 从复用池里取出长度一致的数组
     */
    fun get(len: Int): ByteArray

    /**
     * 往复用池里面添加
     */
    fun put(data: ByteArray)

    /**
     * 清理所有缓存
     */
    fun clearMemory()

    /**
     * 对缓存进行裁剪，可以理解为不断抛出对象知道符合大小为止
     * @param level 裁剪完成后的新长度
     */
    fun trimMemory(level: Int)

    /**
     * 获取最大大小
     */
    fun getMaxSize(): Int
}