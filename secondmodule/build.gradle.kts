/*plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}*/
plugins {
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jormun.secondmodule"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        applicationId = "com.jormun.secondmodule"
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        //consumerProguardFiles("consumer-rules.pro")
        ndk {
            abiFilters.add("armeabi-v7a")
        }
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

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso)
    //implementation(project(":likeshadow-core"))
    implementation("com.jormun.likeshadow:shadowCore:1.0.0")
    implementation(project(":likeshadow"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

