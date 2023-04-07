package com.jormun.retrofit

//请求需要添加参数，而这个注解就是标记请求的入参
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Field(val value: String = "")
