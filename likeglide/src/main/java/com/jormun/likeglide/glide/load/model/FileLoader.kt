package com.jormun.likeglide.glide.load.model

import android.net.Uri
import java.io.File
import java.io.InputStream

class FileLoader<Data>(private val loader: ModelLoader<Uri, Data>) : ModelLoader<File, Data> {

    override fun handles(model: File): Boolean {
        return true
    }

    override fun buildLoadData(model: File): ModelLoader.LoadData<Data>? {
        return loader.buildLoadData(Uri.fromFile(model))
    }

    class Factory : ModelLoader.ModelLoaderFactory<File, InputStream> {
        override fun build(registry: ModelLoaderRegistry): ModelLoader<File, InputStream> {
            return FileLoader(registry.build(Uri::class.java, InputStream::class.java))
        }
    }
}