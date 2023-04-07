package com.jormun.likeroom.an

//用来标记db列名以及列值
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class DbField(val fieldName: String)
