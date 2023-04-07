//低于8.1会报错，需要用注解压制
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    /*id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    扔给根工程的build处理。*/
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.jormun.likeroom"
    compileSdk = libs.versions.compileSdk.get().toInt()
    //接收根build.kts透传过来的参数：
    val isApplication = project.extra[project.name] as Boolean
    //等同于defaultConfig{...}
    defaultConfig.apply {
        minSdk = libs.versions.minSdk.get().toInt()
        if (isApplication) {
            applicationId = "com.jormun.likeroom"
            targetSdk = libs.versions.targetSdk.get().toInt()
            versionCode = libs.versions.versionCode.get().toInt()
            versionName = libs.versions.versionName.get()
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    //app跟library使用不同的AndroidManifest.xml
    if (isApplication) {
        sourceSets.getByName("main").manifest.srcFile("src/main/manifest/AndroidManifest.xml")
    } else {
        sourceSets.getByName("main").manifest.srcFile("src/main/AndroidManifest.xml")
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
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso)
    implementation(project(":likerouter-annotation"))
    implementation(project(":likerouter"))
    ksp(project(":likerouter-compiler"))
}