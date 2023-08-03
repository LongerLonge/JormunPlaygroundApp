package com.jormun.likeglide.glide.load.codec

/**
 * 对图片进行解码的解码注册器。
 * 因为可能存在多个图片解码器，这种方式可以省去一堆if else判断。
 * 好处参见
 * @see com.jormun.likeglide.glide.load.model.ModelLoaderRegistry
 */
class ResourceDecoderRegistry {
    /**
     * 内部封装类，就是把数据再一步封装为一个类
     */
    class Entry<T>(
        private val dataClass: Class<T>,
        val decoder: ResourceDecoder<T>
    ) {
        /**
         * 判断是否需要处理，过滤用
         */
        fun handles(dataClass: Class<*>): Boolean {
            return this.dataClass.isAssignableFrom(dataClass)
        }
    }

    /**
     * 封装类列表
     */
    private val entries = mutableListOf<Entry<*>>()

    /**
     * 把解码器添加到列表中。
     * @param dataClass 数据类型，比如InputStream
     * @param decoder 对应的解码器
     */
    fun <T> add(dataClass: Class<T>, decoder: ResourceDecoder<T>) {
        entries.add(Entry(dataClass, decoder))
    }

    /**
     * 根据数据类型获取对应的解码器。
     * 可能为一个列表。
     * 列表里面也可能只有一个。
     * @param dataClass 数据类型，比如InputStream
     * @return 返回list里面可能有一个或多个
     */
    fun <Data> getDecoders(dataClass: Class<Data>): List<ResourceDecoder<Data>> {
        val decoders = mutableListOf<ResourceDecoder<Data>>()
        for (entry in entries) {
            if (entry.handles(dataClass)) {
                decoders.add(entry.decoder as ResourceDecoder<Data>)
            }
        }
        return decoders
    }
}