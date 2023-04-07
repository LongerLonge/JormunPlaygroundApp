package com.jormun.likeroom.an

//标记哪个字段为主键
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class DbMainKey()
