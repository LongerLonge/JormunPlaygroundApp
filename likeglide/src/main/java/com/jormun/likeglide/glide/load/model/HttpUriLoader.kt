package com.jormun.likeglide.glide.load.model

import android.net.Uri
import com.jormun.likeglide.glide.bean.ObjectKey
import com.jormun.likeglide.glide.load.model.ModelLoader
import com.jormun.likeglide.glide.load.model.data.HttpUriFetcher
import java.io.InputStream

/**
 * 协助HttpUriFetcher工作的辅助类
 */
class HttpUriLoader : ModelLoader<Uri, InputStream> {


    override fun handles(uri: Uri): Boolean {
        return uri.scheme.equals("https", true)
    }

    override fun buildData(uri: Uri): ModelLoader.LoadData<InputStream> {
        return ModelLoader.LoadData(ObjectKey(uri), HttpUriFetcher(uri))
    }

    //实现工厂接口
    class Factory : ModelLoader.ModelLoaderFactory<Uri, InputStream> {
        override fun build(registry: ModelLoaderRegistry): ModelLoader<Uri, InputStream> {
            return HttpUriLoader()//不需要用到ModelLoaderRegistry，自己创建自己
        }
    }
}