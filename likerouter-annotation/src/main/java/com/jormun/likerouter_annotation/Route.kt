package com.jormun.likerouter_annotation

/**
 * Kotlin注解：
 * SOURCE:
 * 表示注解仅保留在源代码中，编译器将丢弃该注解。
 * BINARY:
 * 参与编译，但是编译完后丢弃，不会保留在编译完后的class里。
 * RUNTIME:
 * 参与编译且编译完后继续保留在class文件里，让程序在运行的时候也能找到这个注解。
 *
 *
 * Java注解：
 * 1.SOURCE:在源文件中有效（即源文件保留）
 * 2.CLASS:在class文件中有效（即class保留）
 * 3.RUNTIME:在运行时有效（即运行时保留）
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Route(
    val path: String,//路径
    val group: String = ""//分组，默认空为不分
)
