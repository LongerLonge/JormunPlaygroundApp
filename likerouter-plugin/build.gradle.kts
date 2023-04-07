plugins {
    id("kotlin")
    id("groovy")
    id("java-gradle-plugin")
    id("maven-publish")
}

dependencies {
    implementation("com.android.tools.build:gradle:7.4.2")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.javassist:javassist:3.20.0-GA")
}

//用来标记插件的入口类，免去在resources里面定义：
gradlePlugin {
    plugins {
        create("routerRegister") {
            id = "router-register"// 在 app 模块需要通过 id 引用这个插件
            implementationClass = "com.jormun.likerouter_plugin.RouterPlugin" // 实现这个插件的类的路径
        }
    }
}


//下面这些是为了发布到本地仓库用的：
publishing {
    // 定义发布什么
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.jormun.plugin"
            artifactId = "routerRegister"
            version = "1.0.0"
            from(components["java"])
        }
    }
}