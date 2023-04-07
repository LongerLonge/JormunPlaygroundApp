// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.gradle.*

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
        classpath("com.android.tools.build:gradle:7.4.0")
        classpath("com.jormun.plugin:routerRegister:1.0.0")
    }
    //重点，可以把代码延伸到另外一个文件
    apply(from = "config.gradle.kts")
}

//https://github.com/gradle/gradle/issues/22797
//不加这行会报错，gradle 8.1 已修复
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply (false)
    alias(libs.plugins.ksp) apply (false)
    //id("router-register")  apply (false)
    //println("this is app plugin: ${libs.plugins.android.application.get().pluginId}")
}

//val isApplication by extra { false }

subprojects {
    //subprojects里面project.extra是每个子Module的project参数，并不是全局参数，要注意！
    //如果要全局共享数据，建议用rootProject.extra
    println("${rootProject.extra[project.name]}")
    project.extra.set(project.name, false)//设置这玩意可以透传给子Module的build.kts
    //根据子project配置来获取是否需要依赖Android包
    when (val type = rootProject.extra[project.name] as Int) {
        0 -> {
            project.extra.set(project.name, true)
            println("application true: ${project.name}")
            //println("this is app plugin: ${libs.plugins.android.application.get().pluginId}")
            //apply(plugin = "com.android.application")//作为Application使用
            apply(plugin = rootProject.libs.plugins.android.application.get().pluginId)//作为Application使用
            apply(plugin = "kotlin-android")//这个默认自带的，不需要再toml定义
        }
        1 -> {
            println("library true: ${project.name}")
            //apply(plugin = "com.android.library")//作为Library使用
            apply(plugin = rootProject.libs.plugins.android.library.get().pluginId)//作为Library使用
            apply(plugin = "kotlin-android")//这个默认自带的，不需要再toml定义
        }
        2 -> {
            //apply(plugin = "org.jetbrains.kotlin.jvm")//普通kt/java Library就用这个}
            apply(plugin = rootProject.libs.plugins.kotlin.jvm.get().pluginId)//普通kt/java Library就用这个
        }
    }
    //给所有子Module指定编译的版本，jdk11
    pluginManager.withPlugin("java-library") {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}

tasks.register<Delete>(name = "clean") {
    group = "build"
    delete(rootProject.buildDir)
}

