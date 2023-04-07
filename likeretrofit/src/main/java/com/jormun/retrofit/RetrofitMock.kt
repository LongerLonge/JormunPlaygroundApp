package com.jormun.retrofit

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

//负责处理各种杂事，对外暴露的api。
// 不是发起请求的那个，这个类不负责发起请求。
class RetrofitMock(var baseUrl: String = "") {

    //把注解上的Method解析成我们的ServiceMethod，然后缓存起来
    val serviceMethodMap = mutableMapOf<Method, ServiceMethod>()

    //把Server类转换成实体类，通过代理的方式。
    //本质上就是把Server这个抽象接口转换为实体类，才能参与调用。
    fun <T> create(service: Class<T>): T {//*****泛型用法！
        return Proxy.newProxyInstance(
            service.classLoader,
            arrayOf(service),
            object : InvocationHandler {//****匿名对象！
            override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>): Any? {
                try {
                    val loadServiceMethod = loadServiceMethod(method, args)
                    val rfOkhttpCall = RfOkhttpCall(loadServiceMethod)
                    return rfOkhttpCall
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
                return null
            }
            }) as T
    }

    private fun loadServiceMethod(method: Method, args: Array<out Any?>): ServiceMethod {
        //***判空如果有则返回没有则运行下面run代码！
        return serviceMethodMap[method] ?: run {
            val serviceMethod = ServiceMethod(this, method, args)
            serviceMethodMap[method] = serviceMethod
            serviceMethod
        }
    }

}