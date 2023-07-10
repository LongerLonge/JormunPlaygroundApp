package com.jormun.likeglide.glide.load.model

import android.net.Uri
import java.io.File
import java.io.InputStream
import java.lang.Exception

/**
 * 大类型判断，假如是String就用这个
 * String类型可以是网络的url或者是本地文件的路径
 * @param modelLoader 因为我们不知道是哪种类型，所以需要调用者提供一个具体的loader来进行数据的加载。
 *                    比如是本地文件就用传FileUriLoader
 *                    网络的图片Url就HttpUriLoader
 */
class StringModelLoader(private val modelLoader: ModelLoader<Uri, InputStream>) :
    ModelLoader<String, InputStream> {
    override fun handles(model: String): Boolean {
        return true
    }


    override fun buildData(model: String): ModelLoader.LoadData<InputStream>? {
        //这里我们只是简单的对String进行Uri解析，随后就交给指定的Loader自己去build。
        val uri: Uri?
        try {
            uri = if (model.startsWith("/")) {
                Uri.fromFile(File(model))
            } else {
                Uri.parse(model)
            }
            return this.modelLoader.buildData(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    //实现了工厂接口来生产对象
    class Factory : ModelLoader.ModelLoaderFactory<String, InputStream> {
        override fun build(registry: ModelLoaderRegistry): ModelLoader<String, InputStream> {
            //注意这里是用ModelLoaderRegistry帮忙创建的，而且传入的是Uri和InputStream
            //因为我们已经知道了String类型无非就是File路径或者Http路径，所以我们直接用Uri和InputStream创建就好了
            return StringModelLoader(registry.build(Uri::class.java, InputStream::class.java))
        }

    }
}