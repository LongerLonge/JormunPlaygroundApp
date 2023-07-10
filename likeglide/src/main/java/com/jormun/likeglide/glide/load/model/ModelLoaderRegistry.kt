package com.jormun.likeglide.glide.load.model

import java.lang.Exception

/**
 * 注册Loader并且根据判断返回对应的Loader
 */
class ModelLoaderRegistry {
    //保存封装ModelLoaderFactory的Entry对象数组
    private val entryList = mutableListOf<Entry<*, *>>()

    /**
     * 添加或者说注册Loader到数组中
     * @param modelClass 请求的参数类型，比如String.class
     * @param dataClass  需要返回的类型，比如InputStream.class
     * @param factory  ModelLoader.ModelLoaderFactory，用来匹配类型和输出，假如命中就调用这个Factory来创建
     *                 对应的Loader
     */
    @Synchronized
    fun <Model, Data> add(
        modelClass: Class<Model>,
        dataClass: Class<Data>,
        factory: ModelLoader.ModelLoaderFactory<Model, Data>
    ) {
        //注意加入的是Entry，也就是ModelLoaderFactory的包装类。
        entryList.add(Entry(modelClass, dataClass, factory))
    }

    /**
     * 根据给定的请求参数和返回类型，创建对应的Loader。
     * @param modelClass 请求参数类型，比如String.class
     * @param dataClass 需要返回的类型，比如InputStream.class
     */
    fun <Model, Data> build(
        modelClass: Class<Model>,
        dataClass: Class<Data>
    ): ModelLoader<Model, Data> {
        val loaders = mutableListOf<ModelLoader<Model, Data>>()//可能有多个，比如Uri.class
        for (entry in entryList) {
            if (entry.handles(modelClass, dataClass)) {
                //注意创建是用里面的factory帮忙创建的，并不是直接new。
                loaders.add(entry.factory.build(this@ModelLoaderRegistry) as ModelLoader<Model, Data>)
            }
        }

        if (loaders.isEmpty()) {
            throw Exception("No match: ${modelClass.name} Data: ${dataClass.name}")
        }

        return if (loaders.size > 1) {
            MultiModelLoader(loaders)
        } else {
            loaders[0]
        }
    }

    /**
     * 根据给定的请求参数类型，获取对应的Loader
     * @param modelClass 请求参数类型，比如String.class
     * @return 因为可能有多个，所以返回一个List，举个例子就是文件路径和网络路径都是String类型，所以会返回两个Loader。
     */
    fun <Model> getModelLoaders(modelClass: Class<Model>): List<ModelLoader<Model, *>> {
        val loaders = mutableListOf<ModelLoader<Model, *>>()
        for (entry in entryList) {
            if (entry.handles(modelClass)) {
                loaders.add(entry.factory.build(this@ModelLoaderRegistry) as ModelLoader<Model, *>)
            }
        }
        return loaders
    }

    /**
     * 封装了ModelLoader.ModelLoaderFactory的封装类，用来判断是否为指定的请求类型+返回类型的Loader。
     * @param factory 这个是用来创建Loader的。
     */
    class Entry<Model, Data>(
        private val modelClass: Class<Model>, private val dataClass: Class<Data>,
        val factory: ModelLoader.ModelLoaderFactory<Model, Data>
    ) {
        fun handles(modelClass: Class<*>, dataClass: Class<*>): Boolean {
            //a.isAssignableFrom(b)，是判断类a是否为类b的父类或者是同一个类，是则返回true。(孙子类也返回true)
            //或者假设类a是接口，则判断类b是否为类a的实现类(孙子类也返回true)。
            return this.modelClass.isAssignableFrom(modelClass)
                    && this.dataClass.isAssignableFrom(dataClass)
        }

        fun handles(modelClass: Class<*>): Boolean {
            return this.modelClass.isAssignableFrom(modelClass)
        }
    }
}