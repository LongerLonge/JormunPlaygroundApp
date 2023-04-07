package com.jormun.retrofit

@Retention(AnnotationRetention.RUNTIME)//作用在运行时，也就是app运行时间动态生成
@Target(AnnotationTarget.FUNCTION)//作用在函数上
annotation class GET(val url: String = "")
