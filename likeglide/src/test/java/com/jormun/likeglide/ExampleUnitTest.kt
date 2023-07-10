package com.jormun.likeglide

import android.net.Uri
import com.jormun.likeglide.glide.load.model.FileUriLoader
import com.jormun.likeglide.glide.load.model.HttpUriLoader
import com.jormun.likeglide.glide.load.model.ModelLoader
import com.jormun.likeglide.glide.load.model.ModelLoaderRegistry
import com.jormun.likeglide.glide.load.model.StringModelLoader
import com.jormun.likeglide.glide.load.model.data.DataFetcher
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.InputStream
import java.lang.Exception

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

//@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testLoader() {
        //part 1.
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
        //part 2.
        val modelLoaders = modelLoaderRegistry.getModelLoaders(String::class.java)
        val modelLoader = modelLoaders[0]
        //part 3.
        val buildData = modelLoader.buildData("https") as ModelLoader.LoadData<InputStream>
        //part 4.
        buildData.fetcher?.loadData(object :
            DataFetcher.DataFetcherCallBack<InputStream> {
            override fun onFetcherReady(data: InputStream) {

            }

            override fun onLoadFailed(e: Exception) {

            }
        })
    }
}