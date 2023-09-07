package com.jormun.likeglide.glide.load.generator

import android.util.Log
import com.jormun.likeglide.glide.GlideContext
import com.jormun.likeglide.glide.bean.Key
import com.jormun.likeglide.glide.cache.take.DiskCache
import com.jormun.likeglide.glide.load.model.ModelLoader
import com.jormun.likeglide.glide.load.model.data.DataFetcher
import java.io.File

/**
 * 从缓存中生成数据的类。
 * 说是生成，实际上就是去磁盘缓存里面找看看有没有。
 * 如果有，就提取出来，然后让对应的Loader转换成需要的数据类型。
 * 这个类要注意是如何唯一定位资源的，就是通过ObjectKey。
 *
 * @param model model指的就是输入数据，比如可以是File类型或者一串String类型的url。
 * @param cb 接口，回调用，在这个例子里面是DataGenerator被DecodeJob监听
 */
class DataCacheGenerator(
    private var glideContext: GlideContext,
    private var diskCache: DiskCache,
    private var model: Any,
    private var cb: DataGenerator.DataGeneratorCallback
) : DataGenerator, DataFetcher.DataFetcherCallBack<Any> {
    private val TAG = "DataCacheGenerator"

    //数据加载的Loader
    private var modelLoaders: List<ModelLoader<File?, *>>? = null

    //对应数据类型的唯一Key，用于定位，用列表也只是为了防止多个，正常情况下只有一个
    private var keys = mutableListOf<Key>()

    private var sourceIdIndex = -1//用来遍历Key列表的index
    private var sourceKey: Key? = null//缓存数据的Key，回调时会赋值，在数据层用的是ObjectKey这点要注意
    private var modelLoaderIndex = 0//用来遍历ModelLoader的index
    private var cacheFile: File? = null//假如找到缓存就赋值给这个

    //LoadData，真正的数据加载执行者。
    private var loadData: ModelLoader.LoadData<Any>? = null

    init {
        //假设传的是String类型的url，比如“http://1.img”，那么就拿这个去构建LoadData，并且封装ObjectKey进去。
        //可以简单理解为就是把这一串url按照类型，封装成一个ObjectKey返回。
        //给个list也只是为了防止有多个，什么情况下会有多个？貌似我也没头绪，只要记住99%的情况下都只有一个就行了。
        keys = glideContext.registry.getKeys(model)
    }


    override fun startNext(): Boolean {
        Log.e(TAG, "磁盘加载器开始加载")
        //负责将 注册的model 转换为需要的 data
        // 我们注册了 将http地址/文件地址 转化为InputStream

        //while循环做了两件事：1、遍历Key列表找到是否命中磁盘缓存 2、根据Key找到的数据构建Loader用来进行数据转换。
        while (modelLoaders == null) {
            sourceIdIndex++
            //所有Key都找不到匹配的，说明不在磁盘缓存里，中止while循环，直接返回false
            if (sourceIdIndex >= keys.size) {
                return false
            }
            //keys列表实际上只有一个值，就是我们传入进来的某个url(被封装成ObjectKey)
            val sourceId = keys[sourceIdIndex]
            //看看这个Key在磁盘缓存里面存在吗？
            cacheFile = diskCache[sourceId]
            Log.e(TAG, "磁盘缓存存在则位于:${cacheFile}")
            if (cacheFile != null) {
                //存在则把其取出来，通过Loader来转成我们需要的类型。
                //比如这里，我们保存在磁盘里面的缓存是File类型，那么就是
                // (model: File) ->>(loader)->> (data: InputStream)
                sourceKey = sourceId
                Log.e(TAG, "获得所有文件加载器")
                //获得所有的文件加载转换器
                modelLoaders = glideContext.registry.getModelLoaders(cacheFile)
                modelLoaderIndex = 0
            }
        }

        var started = false
        //因为加载转换器可能有多个，我们这里需要筛选出正确的那个来处理。
        //如String类型的就可以传url或者本地文件的地址，但是它们同属于String -> InputStream加载转换器，所以需要
        //筛选出正确的那个来执行，关于如何筛选可以看: MultiModelLoader
        while (!started && hasNextModelLoader()) {//遍历加载转换器列表
            val modelLoader = modelLoaders!![modelLoaderIndex++]
            //如果是多个，实际上在buildData过程中就进行了筛选，见 MultiModelLoader
            loadData = modelLoader.buildLoadData(cacheFile!!) as ModelLoader.LoadData<Any>?
            Log.e(TAG, "获得加载设置数据")
            //找到对应的加载转换器后，因为加载完后需要解码Bitmap，那么要看看能否找到对应的类型转换解码器(这里不负责解码)
            if (loadData != null && glideContext.registry.hasLoadPath(loadData!!.fetcher!!.getDataClass())) {
                Log.e(TAG, "加载设置数据输出数据对应能够查找有效的解码器路径,开始加载数据")
                started = true
                //找到后，直接执行加载转换并且让接口回调
                loadData!!.fetcher!!.loadData(this)
            }
        }
        return started
    }

    private fun hasNextModelLoader(): Boolean {
        return modelLoaderIndex < modelLoaders!!.size
    }

    override fun cancel() {
        loadData?.fetcher?.cancel()
    }

    override fun onFetcherReady(data: Any) {
        Log.e(TAG, "加载器加载数据成功回调")
        cb.onDataReady(sourceKey, data, DataGenerator.DataGeneratorCallback.DataSource.CACHE)
    }

    override fun onLoadFailed(e: Exception) {
        Log.e(TAG, "加载器加载数据失败回调")
        cb.onDataFetcherFailed(sourceKey, e)
    }
}