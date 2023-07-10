package com.jormun.likeglide.glide.load.model.data

import android.net.Uri
import android.util.Log
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 从网络加载图片的具体实现类，继承自DataFetcher
 * 也可以称为网络图片数据提供者
 */
class HttpUriFetcher(private val uri: Uri) : DataFetcher<InputStream> {

    private val TAG = "HttpUriFetcher"
    private var isCancel = false

    override fun loadData(callBack: DataFetcher.DataFetcherCallBack<InputStream>) {
        /*Log.e(TAG, "loadData: test")
        return*/
        var urlConnection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        try {
            val url = URL(uri.toString())
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connect()
            inputStream = urlConnection.inputStream
            val responseCode = urlConnection.responseCode
            if (isCancel) return
            if (responseCode == HttpURLConnection.HTTP_OK) {
                callBack.onFetcherReady(inputStream)
            } else {
                callBack.onLoadFailed(Exception("loadData err: responseCode is $responseCode"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "loadData err: ${e.message}")
            e.printStackTrace()
            callBack.onLoadFailed(e)
        } finally {
            inputStream?.apply {
                try {
                    close()
                } catch (e: Exception) {
                    Log.e(TAG, "loadData: close inputStream failed.")
                    e.printStackTrace()
                }
            }
            urlConnection?.disconnect()
        }
    }


    override fun cancel() {
        isCancel = true
    }
}