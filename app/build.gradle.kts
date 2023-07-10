
//低于8.1会报错，需要用注解压制
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    /*id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    扔给根工程的build处理。*/
    alias(libs.plugins.ksp)
    id("router-register")
}

android {
    namespace = "com.jormun.playground"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jormun.playground"
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

//KSP: 指定编译器在编译时顺便把这个参数透传给Processor：
ksp {
    arg("ROUTER_MODULE_NAME", project.name)
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(project(":likeshadow"))
    implementation(project(":likeretrofit"))
    implementation(project(":likeroom"))
    implementation(project(":likerouter-annotation"))
    implementation(project(":likerouter"))
    implementation(project(":likeglide"))
    ksp(project(":likerouter-compiler"))
}