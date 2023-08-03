package com.jormun.likeglide.glide.load.model

/**
 * 假设有多个Model.class类型一致的Loader，用这个类进行包装并且返回对应的Loader(通过handles判断)。
 * 在这个例子里面，因为File Url和Http Url都是String的类型，所以会有两个Loader被包装进去。
 * 遍历并且通过handles来获取我们想要的那个Loader，比如判断是https的就返回HttpLoader，如果是file://就返回FileLoader
 */
class MultiModelLoader<Model, Data>(private val modelLoaders: List<ModelLoader<Model, Data>>) :
    ModelLoader<Model, Data> {
    override fun handles(model: Model): Boolean {
        for (modelLoader in modelLoaders) {
            if (modelLoader.handles(model)) {
                return true
            }
        }
        return false
    }
    //筛选出对应的Loader来构建Loader.Data。
    override fun buildLoadData(model: Model): ModelLoader.LoadData<Data>? {
        for (modelLoader in modelLoaders) {
            if (modelLoader.handles(model)) {
                return modelLoader.buildLoadData(model)
            }
        }
        return null
    }
}