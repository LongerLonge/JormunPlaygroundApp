package com.jormun.retrofit

import okhttp3.*
import java.io.IOException

//用Okhttp来真正发起请求，创建时需要提供转换好的ServiceMethod
//这个类的职责是真正的用Okhttp来发起请求。
//通过与RfCall接口组合来实现请求的功能，而不是用抽象类或者抽象方法来实现，因为组合的耦合性要比直接定死在类里要高。
class RfOkhttpCall(val serviceMethod: ServiceMethod) : RfCall {
    companion object {
        private val client: OkHttpClient = OkHttpClient()
    }

    override fun execute(): String {
        return if (serviceMethod.methodName == "GET") {
            val request = Request.Builder().url(serviceMethod.getRequestUrl()).build()
            val response = client.newCall(request).execute()
            response.body!!.string()
        } else
            "not post now！"
    }

    override fun enqueue(callback: Callback) {
        if (serviceMethod.methodName == "GET") {
            val request = Request.Builder().url(serviceMethod.getRequestUrl()).build()
            val response = client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback.onFailure(call, e)
                }

                override fun onResponse(call: Call, response: Response) {
                    callback.onResponse(call, response)
                }
            })
        } else {
            "not post now!"
        }
    }

    override suspend fun enqueue() : String{
        return if (serviceMethod.methodName == "GET") {
            val request = Request.Builder().url(serviceMethod.getRequestUrl()).build()
            val response = client.newCall(request).execute()
            response.body!!.string()
        } else
            "not post now！"
    }

}