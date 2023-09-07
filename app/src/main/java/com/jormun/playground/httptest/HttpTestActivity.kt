package com.jormun.playground.httptest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.jormun.likehttp.Call
import com.jormun.likehttp.Callback
import com.jormun.likehttp.LikeHttpClient
import com.jormun.likehttp.Response
import com.jormun.likehttp.net.Request
import com.jormun.playground.R

class HttpTestActivity : AppCompatActivity() {

    val TAG = "HttpTestActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_http_test)
    }

    fun testLikeHttp(view: View) {
        val likeHttpClient = LikeHttpClient()
        val url = "https://www.baidu.com"
        val request = Request.Builder().setUrl(url).get().build()
        val newCall = likeHttpClient.newCall(request)
        newCall.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                Toast.makeText(this@HttpTestActivity, "onResponse: 请求成功: ${response.code}", Toast.LENGTH_SHORT).show()

            }

            override fun onFailure(call: Call, throwable: Throwable) {
                throwable.printStackTrace()
                Toast.makeText(this@HttpTestActivity, "onFailure: 请求失败: ${throwable.message}", Toast.LENGTH_SHORT).show()
            }

        })
    }
}