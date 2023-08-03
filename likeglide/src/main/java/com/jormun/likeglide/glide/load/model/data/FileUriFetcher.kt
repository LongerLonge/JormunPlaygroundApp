package com.jormun.likeglide.glide.load.model.data

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream

/**
 * 真正执行本地文件数据的类，继承自DataFetcher。
 * @param contentResolver 需要一个ContentResolver来通过Uri或者本地文件的InputStream。
 */
class FileUriFetcher(private val uri: Uri, private val contentResolver: ContentResolver) :
    DataFetcher<InputStream> {
    override fun loadData(callBack: DataFetcher.DataFetcherCallBack<InputStream>) {
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                throw Exception("FileUriFetcher loadData failed: get file is null.")
            }
            callBack.onFetcherReady(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            callBack.onLoadFailed(e)
        } finally {
            inputStream?.apply {
                try {
                    close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun cancel() {
        //什么都不需要做。
    }

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }
}