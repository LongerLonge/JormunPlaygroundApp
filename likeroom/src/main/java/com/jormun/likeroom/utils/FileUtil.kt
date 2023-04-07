package com.jormun.likeroom.utils

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

const val FILE_UTILS_TAG = "FILE_UTILS_TAG"

object FileUtil {

    /**
     * 复制单个文件(可更名复制)
     * @param oldPathFile 准备复制的文件源
     * @param newPathFile 拷贝到新绝对路径带文件名(注：目录路径需带文件名)
     * @return
     */
    fun copySingleFile(oldPathFile: String, newPathFile: String) {
        if (oldPathFile.isEmpty() || newPathFile.isEmpty()) {
            Log.e(FILE_UTILS_TAG, "CopySingleFile err: path not null!")
            return
        }
        try {
            //  int bytesum = 0;
            var byteread = 0
            val oldfile = File(oldPathFile)
            val newFile = File(newPathFile)
            val parentFile: File = newFile.getParentFile()
            if (!parentFile.exists()) {
                parentFile.mkdirs()
            }
            if (oldfile.exists()) { //文件存在时
                val inStream: InputStream = FileInputStream(oldPathFile) //读入原文件
                val fs = FileOutputStream(newPathFile)
                val buffer = ByteArray(1024)
                while (inStream.read(buffer).also { byteread = it } != -1) {
                    //bytesum += byteread; //字节数 文件大小
                    fs.write(buffer, 0, byteread)
                }
                inStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}