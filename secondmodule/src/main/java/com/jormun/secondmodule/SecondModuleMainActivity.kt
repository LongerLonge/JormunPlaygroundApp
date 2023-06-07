package com.jormun.secondmodule

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jormun.likeshadow.PluginContainerActivity
import com.jormun.likeshadow.PluginManagerImpl
import com.jormun.likeshadow_core.ShadowContext
import java.io.File
import android.Manifest
import android.content.res.Resources

class SecondModuleMainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_module_main)
        checkPermission()
    }


    private fun checkPermission(): Boolean {
         if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
             requestPermissions(
                 arrayOf(
                     Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE
                 ), 1
             )
         }
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

    fun loadApk(view: View) {
        val apkFile = File(Environment.getExternalStorageDirectory(), "launcher-release.apk")
        PluginManagerImpl.sInstance.setHostContext(this)
        PluginManagerImpl.sInstance.loadPluginApkPath(apkFile.absolutePath)
    }

    fun openApk(view: View) {
        val apkFile = File(Environment.getExternalStorageDirectory(), "launcher-release.apk")
        val intent = Intent(this, PluginContainerActivity::class.java)
        intent.putExtra("apk", apkFile.absolutePath)
        startActivity(intent)
    }

    override fun getResources(): Resources {
        val pluginApkResources = PluginManagerImpl.sInstance.getPluginApkResources()
        return pluginApkResources ?: super.getResources()
    }
}