package com.jormun.playground.splugin

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.jormun.likeshadow.PluginContainerActivity
import com.jormun.likeshadow.PluginManagerImpl
import com.jormun.playground.R
import java.io.File

class PluginApkTestActivity : AppCompatActivity() {
    private val hahah = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_plugin_apk)
        checkPermission()
    }

    private fun checkPermission(): Boolean {
        /* if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
             requestPermissions(
                 arrayOf(
                     Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE,
                     Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                 ), 1
             )
         }*/
        //因为插件apk是保存在sdk中，所以需要获取操作sd卡的权限，这里只是为了读取目标apk并无做其它操作：
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            Environment.isExternalStorageManager()
        ) {
            Toast.makeText(this, "已获得访问所有文件权限", Toast.LENGTH_SHORT).show()
        } else {
            val builder = AlertDialog.Builder(this)
                .setMessage("本程序需要您同意允许访问所有文件权限")
                .setPositiveButton("确定") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            builder.show()
        }
        return false
    }

    fun loadPluginApk(view: View) {
        val apkFile = File(Environment.getExternalStorageDirectory(), "secondmodule-debug.apk")
        PluginManagerImpl.sInstance.setHostContext(this)
        PluginManagerImpl.sInstance.loadPluginApkPath(apkFile.absolutePath)
        //抽取出apk的启动LaunchActivity，然后启动
    }

    fun startPlugin(view: View) {
        //val apkFile = File(Environment.getExternalStorageDirectory(), "secondmodule-debug.apk")
        val intent = Intent(hahah, PluginContainerActivity::class.java)
        //intent.putExtra("apk", apkFile.absolutePath)
        //startActivity(intent)
    }
}