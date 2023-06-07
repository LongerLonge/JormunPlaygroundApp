//这个文件里的内容必须得是在正确的函数域里面使用。
//比如这个rootProject.extra只能在build文件的buildscript{。。。}使用。
//在setting.gradle.kts 的 pluginManagement{。。。}里面就用不了。

//各个Module在这里配置状态
//0是app
//1是Android Library
//2是普通kt\java Library
mapOf(
    Pair("app", 0),
    Pair("likeretrofit", 2),
    Pair("likerouter-annotation", 2),
    Pair("likerouter", 1),
    Pair("likerouter-compiler", 2),
    Pair("likerouter-plugin", 2),
    Pair("secondmodule", 0),
    Pair("likeshadow-core", 1),
    Pair("likeshadow", 1),
    Pair("likeroom", 1)
).entries.forEach {
    //rootProject.extra似乎可以做到全局共享
    rootProject.extra.set(it.key, it.value)
}

