package com.jormun.likeglide.glide.load.model

import com.jormun.likeglide.glide.bean.Key
import com.jormun.likeglide.glide.load.model.data.DataFetcher

/**
 *  描述了数据加载转换器的抽象行为类。
 *  本身并不加载数据，但是可以帮忙创建加载数据的LoadData，而且可以自定义筛选逻辑来决定是否处理某种数据。
 * @param Model 数据来源，有uri有url
 * @param Data 具体数据(被加载后的)，比如InputStream或者Byte[]
 */
interface ModelLoader<Model, Data> {

    /**
     * 帮忙创建Loader的工厂接口
     */
    interface ModelLoaderFactory<Model, Data> {
        /**
         * 创建Loader
         * @param registry 如果需要registry帮忙创建就使用这个参数，不需要就不用。
         * @see com.jormun.likeglide.glide.load.model.StringModelLoader
         */
        fun build(registry: ModelLoaderRegistry): ModelLoader<Model, Data>
    }

    /**
     * 包装了DataFetcher的对象
     * @param key 注意这玩意，这个就是该次加载数据流里面的唯一Key
     * 在加载层是用ObjectKey：
     * @see com.jormun.likeglide.glide.bean.ObjectKey
     * @param fetcher 真正的数据加载执行者，也是加载数据的最小业务类
     */
    class LoadData<Data>(var key: Key, var fetcher: DataFetcher<Data>? = null)


    /**
     * 两个功能
     * 一是对输入的数据进行前置处理
     * 而是对输入的数据进行过滤
     */
    fun handles(model: Model): Boolean

    /**
     * 创建LoadData，里面包装着Fetcher(真正加载数据的最小业务类)
     * @see LoadData
     */
    fun buildLoadData(model: Model): LoadData<Data>?
}
