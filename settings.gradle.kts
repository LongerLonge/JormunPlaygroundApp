include(":secondmodule")


pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        //添加本地仓库
        mavenLocal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}
rootProject.name = "JormunPlaygroundApp"
include(":app")
include(":likeretrofit")
include(":likeroom")


enableFeaturePreview("VERSION_CATALOGS")
include(":likerouter")
include(":likerouter-compiler")
include(":likerouter-plugin")
include(":likerouter-annotation")
include(":likeshadow-core")
include(":likeshadow")
include(":likeglide")
