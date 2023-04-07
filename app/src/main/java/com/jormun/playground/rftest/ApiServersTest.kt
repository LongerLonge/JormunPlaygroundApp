package com.jormun.playground.rftest

import com.jormun.retrofit.Field
import com.jormun.retrofit.GET
import com.jormun.retrofit.RfOkhttpCall

//类似声明清单，代表了api有那些行为。
//本身不能直接参与调用，需要配合RetrofitMock来动态代理生成一个实体类来使用！
interface ApiServersTest {

    @GET("query")
    fun getKuaidiFromServer(@Field("postId") postID: String): RfOkhttpCall
}