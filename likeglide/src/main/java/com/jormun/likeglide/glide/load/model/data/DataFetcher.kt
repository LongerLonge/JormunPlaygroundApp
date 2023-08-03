package com.jormun.likeglide.glide.load.model.data

import java.lang.Exception

/**
 * 真正进行数据加载的抽象层
 * 描述了数据加载者的统一行为
 * 泛型Data只是个代称，你喜欢写T也行，这里只是方便理解
 * Data代指的是加载完毕后回传的数据类型，比如InputStream
 */
interface DataFetcher<Data> {
    /**
     * 数据加载回调接口。
     */
    interface DataFetcherCallBack<Data> {
        fun onFetcherReady(data: Data)
        fun onLoadFailed(e: Exception)
    }

    /**
     * 开始加载数据，传入接口来接受回调
     */
    fun loadData(callBack: DataFetcherCallBack<Data>)

    /**
     *如果加载途中想取消，就用这个
     */
    fun cancel()

    fun getDataClass(): Class<*>
}