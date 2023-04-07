package com.jormun.retrofit

import java.lang.reflect.Method

//对注解进行解析，并且保存对应的参数
class ServiceMethod(val retrofitMock: RetrofitMock, val method: Method, val args: Array<out Any?>) {

    val parameterMap = mutableMapOf<String, Any?>()//参数Map，注意Kotlin的Map默认只能读不能写，需要用mutableMap才可以。
    var methodName = ""//函数名字，这里只分为post和get
    var relativeUrl = ""//api的路径地址，不是baseurl而是后面那串
    val annotations: Array<Annotation> = method.annotations//获取该函数上面的注解,可能有多个，为数组
    val annotatedParameter: Array<Array<Annotation>> =
        method.parameterAnnotations//获取该函数上面的入参注解，同样为多个，数组

    init {//***init函数跟构造函数差不多，都是该对象创建时调用，为Kotlin独有。
        build()
    }

    private fun build() {
        //遍历函数头上面的注解，比如GET POST那些
        for (an in annotations) {
            if (an is GET) {
                methodName = "GET"
                relativeUrl = an.url
            } else if (an is POST) {
                methodName = "POST"
                relativeUrl = an.url
            }
        }

        //遍历函数入参里面的注解
        for ((index, item) in annotatedParameter.withIndex()) {//***withIndex就是遍历时同时取index和item
            val value = args[index]
            for (p in item) {
                if (p is Field) {
                    parameterMap[p.value] = value
                }
            }
        }
    }

    fun getBaseUrl(): String {
        return retrofitMock.baseUrl
    }

    fun getRequestUrl(): String {//拼接URl
        var rawUrl = "${getBaseUrl()}/$relativeUrl"
        if (methodName == "GET") {//如果是get需要拼接参数
            if (parameterMap.isNotEmpty()) {
                rawUrl = buildString {
                    append("$rawUrl?")
                    for (kv in parameterMap) {
                        append(kv.key).append("=").append(kv.value).append("&")
                    }
                    deleteCharAt(this.length - 1)
                }
            }
        }
        return rawUrl
    }

}