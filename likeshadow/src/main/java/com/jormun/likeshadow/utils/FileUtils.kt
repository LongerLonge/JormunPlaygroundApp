package com.jormun.likeshadow.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream


object FileUtils {

    val soList = mutableListOf<String>()

    fun unzipPack(zipPath: String?, outFolder: String, suffix: String?): Boolean {
        var out: FileOutputStream
        val buf = ByteArray(16384)
        return try {
            val zis = ZipInputStream(FileInputStream(zipPath))
            var entry = zis.nextEntry
            while (entry != null) {
                var name = entry.name
                if (entry.isDirectory) {
                    val newDir = File(outFolder + entry.name)
                    newDir.mkdir()
                } else if (name.endsWith(suffix!!)) {
                    var outputFile = File(outFolder + File.separator + name)
                    var outputPath = outputFile.canonicalPath
                    name = outputPath
                        .substring(outputPath.lastIndexOf("/") + 1)
                    outputPath = outputPath.substring(
                        0, outputPath
                            .lastIndexOf("/")
                    )
                    val outputDir = File(outputPath)
                    outputDir.mkdirs()
                    outputFile = File(outputPath, name)
                    outputFile.createNewFile()
                    out = FileOutputStream(outputFile)
                    var numread = 0
                    do {
                        numread = zis.read(buf)
                        if (numread <= 0) {
                            break
                        } else {
                            out.write(buf, 0, numread)
                        }
                    } while (true)
                    out.close()
                    soList.add(outputFile.name)
                }
                entry = zis.nextEntry
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}