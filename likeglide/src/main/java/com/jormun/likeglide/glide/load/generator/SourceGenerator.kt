package com.jormun.likeglide.glide.load.generator

import android.util.Log
import com.jormun.likeglide.glide.GlideContext
import com.jormun.likeglide.glide.load.model.ModelLoader
import com.jormun.likeglide.glide.load.model.data.DataFetcher

/**
 * 通过网络url或者本地文件Url进行数据的加载。
 * 这里默认转换为InputStream了。
 * @param glideContext 上下文，这里是为了获取Registry来获得数据加载器
 * @param model 传入的数据类型，比如一串url。
 * @param cb 回调，这里的例子是DecodeJob接受回调。
 */
class SourceGenerator(
    private var glideContext: GlideContext,
    private var model: Any,
    private var cb: DataGenerator.DataGeneratorCallback
) : DataGenerator, DataFetcher.DataFetcherCallBack<Any> {

    private val TAG = "SourceGenerator"

    //LoadData，真正的数据加载执行者。
    private var loadData: ModelLoader.LoadData<Any>? = null

    private var loadDataListIndex = 0//用来遍历ModelLoaderList的index

    //ModelLoader列表，因为可能有多个符合的ModelLoader
    private var loadDataList: List<ModelLoader.LoadData<*>>? = null

    init {
        //从注册器类里面获取数据加载器
        loadDataList = glideContext.registry.getLoadData(model)
    }

    /**
     * 找一下是否有这个数据类型的ModelLoader，也就是数据加载器。
     * 如果找到对应的ModelLoader加载器就返回true。
     * 找不到就返回false
     */
    override fun startNext(): Boolean {
        Log.e(TAG, "源加载器开始加载")
        var started = false
        if (loadDataList == null || loadDataList!!.isEmpty()) {
            Log.e(TAG, "startNext: not have ModelLoaders! please check, ${model}")
            return started
        }
        while (!started && hasNextModelLoader()) {
            loadData = loadDataList!![loadDataListIndex++] as ModelLoader.LoadData<Any>?
            Log.e(TAG, "获得加载设置数据")
            // hasLoadPath : 是否有个完整的加载路径 从将Model转换为Data之后 有没有一个对应的将Data
            // 转换为图片的解码器，其实就是找找看有没有解码器
            if (loadData != null && glideContext.registry.hasLoadPath(loadData!!.fetcher!!.getDataClass())
            ) {
                Log.e(TAG, "加载设置数据输出数据对应能够查找有效的解码器路径,开始加载数据")
                started = true
                // 将Model转换为Data
                loadData!!.fetcher!!.loadData(this)
            }
        }
        return started
    }

    override fun cancel() {
        loadData?.fetcher?.cancel()
    }

    /**
     * 是否有下一个modelloader支持加载
     * @return
     */
    private fun hasNextModelLoader(): Boolean {
        return loadDataListIndex < loadDataList!!.size
    }

    override fun onFetcherReady(data: Any) {
        Log.e(TAG, "加载器加载数据成功回调")
        cb.onDataReady(loadData?.key, data, DataGenerator.DataGeneratorCallback.DataSource.REMOTE)
    }

    override fun onLoadFailed(e: Exception) {
        Log.e(TAG, "加载器加载数据失败回调")
        cb.onDataFetcherFailed(loadData?.key, e)
    }
}