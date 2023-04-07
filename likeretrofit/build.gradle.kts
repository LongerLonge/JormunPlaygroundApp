//低于8.1会报错，需要用注解压制
/*@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
}*/

dependencies {
    testImplementation(libs.junit)
    implementation(libs.okhttp3)
}

//以下是旧版的gradle文件代码，可以对比来看
/*
plugins {
    id 'org.jetbrains.kotlin.jvm'
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    testImplementation 'junit:junit:4.13.2'
    //implementation "com.google.devtools.ksp:symbol-processing-api:1.7.21-1.0.8"
    implementation "com.squareup.okhttp3:okhttp:4.10.0"
}*/
