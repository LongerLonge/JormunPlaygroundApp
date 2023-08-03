package com.jormun.likeglide.glide.load.model

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.jormun.likeglide.glide.bean.ObjectKey
import com.jormun.likeglide.glide.load.model.data.FileUriFetcher
import java.io.InputStream

/**
 * 协助文件加载者工作的类
 * @param contentResolver 需要一个ContentResolver来通过Uri或者本地文件的InputStream。
 */
class FileUriLoader(private var contentResolver: ContentResolver) : ModelLoader<Uri, InputStream> {

    //根据Uri的scheme来判断是否为文件的前缀。
    override fun handles(uri: Uri): Boolean {
        return ContentResolver.SCHEME_FILE.equals(uri.scheme, true)
    }

    override fun buildLoadData(uri: Uri): ModelLoader.LoadData<InputStream> {
        //注意Uri是在这里被封装成ObjectKey的
        return ModelLoader.LoadData(ObjectKey(uri), FileUriFetcher(uri, contentResolver))
    }


    class Factory(private var contentResolver: ContentResolver) :
        ModelLoader.ModelLoaderFactory<Uri, InputStream> {
        override fun build(registry: ModelLoaderRegistry): ModelLoader<Uri, InputStream> {
            return FileUriLoader(contentResolver)//不需要用到ModelLoaderRegistry，自己创建自己
        }

    }

}