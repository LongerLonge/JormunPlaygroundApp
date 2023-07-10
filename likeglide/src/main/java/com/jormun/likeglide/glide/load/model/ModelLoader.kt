package com.jormun.likeglide.glide.load.model

import com.jormun.likeglide.glide.bean.Key
import com.jormun.likeglide.glide.load.model.data.DataFetcher

/**
 *  对数据加载进行辅助的类
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
     */
    class LoadData<Data>(var key: Key? = null, var fetcher: DataFetcher<Data>? = null)


    /**
     * 判断处理对应model的数据，可以理解为一些前置处理
     */
    fun handles(model: Model): Boolean

    /**
     * 创建对应的数据加载者，这里可以理解为是创建对应的Fetcher(被包装在LoadData里面)
     */
    fun buildData(model: Model): LoadData<Data>?
}
