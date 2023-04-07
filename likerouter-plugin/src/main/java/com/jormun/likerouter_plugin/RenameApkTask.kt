package com.jormun.likerouter_plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class RenameApkTask : DefaultTask() {

    /*   @get:InputFiles
       abstract val allJars: ListProperty<RegularFile>

       @get:InputFiles
       abstract val allDirectories: ListProperty<Directory>

       @get:OutputFile
       abstract val output: RegularFileProperty*/

    @TaskAction
    fun reNameApk() {
        /*val oldApkPath = "/outputs/apk/debug/app-debug.apk"
        val renameApkPath = "/outputs/apk/debug/HelloMyApk.apk"
        val previousApkPath = "${project.buildDir.absoluteFile}$oldApkPath"
        val oldPathFile = File(previousApkPath)

        if (oldPathFile.exists()) {
            val newPath = "${project.buildDir.absoluteFile}$renameApkPath"
            oldPathFile.renameTo(File(newPath))
        } else {
            println("old apk path is not exits!!")
        }*/
        println("Hello, this is my plugin!!! test abstract!!!")
        //ClassPool.getDefault()

    }
}