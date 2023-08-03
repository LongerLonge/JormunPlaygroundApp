package com.jormun.likeglide.glide

import com.jormun.likeglide.glide.bean.Key
import com.jormun.likeglide.glide.load.LoadPath
import com.jormun.likeglide.glide.load.codec.ResourceDecoder
import com.jormun.likeglide.glide.load.codec.ResourceDecoderRegistry
import com.jormun.likeglide.glide.load.model.ModelLoader
import com.jormun.likeglide.glide.load.model.ModelLoaderRegistry

/**
 * 数据加载有多种形式，不同形式又各自有自己的类。
 * 一旦有N种，就会有N个if else，这里则是通过注册模式来实现优化。
 * 数据加载类注册到这里。
 * 图片解码类也注册到这里。
 */
class Registry {
    //负责数据加载的注册类，内部封装了一堆按类型划分的数据加载Loader
    private val modelLoaderRegistry = ModelLoaderRegistry()

    //负责解码的注册类，内部封装了一堆按类型划分的解码器Decoder
    private val resourceDecoderRegistry = ResourceDecoderRegistry()

    /**
     * 添加模型加载器
     *
     * @param source  输入类型
     * @param data    输出类型
     * @param factory 加载器工厂
     * @param <Model> 输入类型泛型
     * @param <Data>  输出类型泛型
     */
    fun <Model, Data> add(
        source: Class<Model>,
        data: Class<Data>,
        factory: ModelLoader.ModelLoaderFactory<Model, Data>
    ): Registry {
        modelLoaderRegistry.add(source, data, factory)
        return this
    }

    /**
     * 获得对应model类型的所有modelloader
     *
     * @param model 对应的数据类，比如String
     * @param <Model> 对应类型的泛型
     * @return 因为可能有多个，所以返回一个List
     * */
    fun <Model> getModelLoaders(model: Model): List<ModelLoader<Model, *>> {
        val modelClass = model!!::class.java as Class<Model>
        return modelLoaderRegistry.getModelLoaders(modelClass)
    }

    /**
     * 根据给定的数据类来获取对应的数据加载器
     * 比如一串String类型的Url: "http://1.img"，就返回String类型的数据加载器。
     * @param model 对应的数据类，比如String
     *
     */
    fun getLoadData(model: Any): List<ModelLoader.LoadData<*>> {
        val loadData = mutableListOf<ModelLoader.LoadData<*>>()
        val modelLoaders: List<ModelLoader<Any, *>> = getModelLoaders(model)
        for (modelLoader in modelLoaders) {
            //创建LoadData
            val current = modelLoader.buildLoadData(model)
            if (current != null) {
                loadData.add(current)
            }
        }
        return loadData
    }

    /**
     * 根据给定的数据类来获取对应的Key列表。
     *  打个比方，假如传入的是一串url，"http://1.img"
     * 那么就会利用这个model的类型去构建LoadData，同时把model数据封装成ObjectKey保存在里面
     * 这就意味着，假如你再次加载上面这个url，就可以定位到唯一的LoadData(里面有一个唯一的ObjectKey)。
     * 这样就可以避免重复加载和利用到正确的缓存
     *
     * 在这里url是被封装成ObjectKey
     * @see com.jormun.likeglide.glide.bean.ObjectKey
     *
     * @param model 传入的数据，比如一串String类型的url
     *
     * @return LoadData中封装的Key，比如ObjectKey
     */
    fun getKeys(model: Any): MutableList<Key> {
        val keys = mutableListOf<Key>()
        //利用model封构建LoadData，同时把model封装成ObjectKey塞到里面
        val loadDatas: List<ModelLoader.LoadData<*>> = getLoadData(model)
        for (loadData in loadDatas) {
            //注意这里只取loadData的Key而已，不需要其它东西
            keys.add(loadData.key)
        }
        return keys
    }

    /**
     * 注册解码加载器
     * @param dataClass 数据类
     * @param decoder 解码器
     */
    fun <T> register(dataClass: Class<T>, decoder: ResourceDecoder<T>) {
        //实际上就是添加到封装类的列表中而已
        resourceDecoderRegistry.add(dataClass, decoder)
    }

    /**
     * 判断当前这个数据类是否有对应的解码器列表。
     * @param dataClass 需要被解码的数据类型，比如InputStream
     */
    fun hasLoadPath(dataClass: Class<*>): Boolean {
        return getLoadPath(dataClass) != null
    }

    /**
     * 获取当前数据类的解码器列表
     * @param dataClass 需要被解码的数据类型，比如InputStream
     * @return 返回的LoadPath是封装了解码器列表的封装类
     */
    fun <Data> getLoadPath(dataClass: Class<Data>): LoadPath<Data> {
        val decoders: List<ResourceDecoder<Data>> = resourceDecoderRegistry.getDecoders(dataClass)
        return LoadPath(dataClass, decoders)
    }


}