plugins {
    id("maven-publish")
}

android {
    namespace = "com.jormun.likeshadow"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    libraryVariants.all {
        outputs.all {
            if (outputFile.name.endsWith(".aar")) {
                logger.warn("${outputFile.absolutePath}-->outputFile!")
                val newAAr = File("$buildDir/outputs/aar/shadowContext-${buildType.name}.aar")
                logger.warn("${newAAr.absolutePath}-->newAAr!")
                val isRenamed = outputFile.renameTo(newAAr)
                logger.warn("isReName: $isRenamed")
            }
        }
    }

}




dependencies {

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(project(":likeshadow-core"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso)
}

//下面这些是为了发布到本地仓库用的：
publishing {
    // 定义发布什么
    publications {
        create<MavenPublication>("ReleaseAar") {
            groupId = "com.jormun.likeshadow"
            artifactId = "shadowContext"
            version = "1.0.0"
            artifact("$buildDir/outputs/aar/${artifactId}-release.aar")
        }
    }
}