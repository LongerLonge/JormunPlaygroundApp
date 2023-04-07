package com.jormun.likerouter_plugin

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class RouterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        /*project.tasks.register("printHello", RenameApkTask::class.java) {
            it.dependsOn("build")
        }*/
        project.extensions.getByType(BaseExtension::class.java)
            .registerTransform(RouterTransform(project))
    }
}