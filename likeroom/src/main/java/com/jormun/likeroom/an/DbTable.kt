package com.jormun.likeroom.an


//用来标记表名和表名的值
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class DbTable(val dbname: String)
