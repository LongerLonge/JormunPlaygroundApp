package com.jormun.likeglide.glide.load.generator

import com.jormun.likeglide.glide.bean.Key

/**
 * 数据生成类的顶层抽象行为接口
 * 负责两种类型的数据生成。
 * 本地-缓存
 * 网络-非缓存
 * 以上两个工作都由不同的实现类负责
 */
interface DataGenerator {
    /**
     * 接口，回调用，在这个例子里面是DataGenerator被DecodeJob监听
     */
    interface DataGeneratorCallback {
        /**
         * 两种类型的标记，本地和网络数据
         * REMOTE
         * CACHE
         */
        enum class DataSource {
            REMOTE, CACHE
        }

        /**
         * DataGenerator回调接口，成功
         * @param key 该数据的Key
         * @param data 数据本身，比如InputStream
         * @param dataSource 数据来源，见
         * @see DataGenerator.DataGeneratorCallback.DataSource
         */
        fun onDataReady(key: Key?, data: Any, dataSource: DataSource)

        /**
         * DataGenerator回调接口，失败
         */
        fun onDataFetcherFailed(key: Key?, e: Exception)
    }

    /**
     * 数据生成器是否可以工作并且启动
     * @return false表示当前这个生成器没法生产数据，true代表可以工作并已启动
     */
    fun startNext(): Boolean

    /**
     * 据生成器关闭
     */
    fun cancel()
}