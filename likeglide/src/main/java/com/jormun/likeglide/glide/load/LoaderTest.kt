package com.jormun.likeglide.glide.load

import android.content.Context
import android.net.Uri
import com.jormun.likeglide.glide.load.model.HttpUriLoader
import com.jormun.likeglide.glide.load.model.ModelLoader
import com.jormun.likeglide.glide.load.model.ModelLoaderRegistry
import com.jormun.likeglide.glide.load.model.StringModelLoader
import com.jormun.likeglide.glide.load.model.data.DataFetcher
import java.io.InputStream
import java.lang.Exception

class LoaderTest {

    private fun test(context: Context) {
        val modelLoaderRegistry = ModelLoaderRegistry()
        modelLoaderRegistry.add(
            String::class.java,
            InputStream::class.java,
            StringModelLoader.Factory()
        )
        modelLoaderRegistry.add(
            Uri::class.java,
            InputStream::class.java,
            HttpUriLoader.Factory()
        )
        /*modelLoaderRegistry.add(
            Uri::class.java,
            InputStream::class.java,
            FileUriLoader.Factory(context.contentResolver)
        )*/

        val modelLoaders = modelLoaderRegistry.getModelLoaders(String::class.java)
        val modelLoader = modelLoaders[0]
        val buildData = modelLoader.buildLoadData("https") as ModelLoader.LoadData<InputStream>
        buildData.fetcher?.loadData(object :
            DataFetcher.DataFetcherCallBack<InputStream> {
            override fun onFetcherReady(data: InputStream) {

            }

            override fun onLoadFailed(e: Exception) {

            }
        })
    }
}