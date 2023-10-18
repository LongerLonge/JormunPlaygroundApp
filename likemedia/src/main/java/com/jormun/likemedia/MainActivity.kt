package com.jormun.likemedia

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column

import androidx.compose.material3.Button

import androidx.compose.material3.Text
import com.jormun.likemedia.codec.LikeMediaCodec
import com.jormun.likemedia.ui.old.OldPlayActivity
import com.jormun.likemedia.ui.old.VideoClipActivity

import com.jormun.likemedia.ui.old.VideoStreamActivity
import java.io.File


class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
        //Log.e(TAG, "onCreate: load h264 file success: ${loadH264File()}")
        setContent {
            Column {
                Button(onClick = {
                    startActivity(Intent(this@MainActivity, PlayVideoActivity::class.java))
                }) {
                    Text(text = "Play")
                }
                Button(onClick = {
                    startActivity(Intent(this@MainActivity, RecordActivity::class.java))
                }) {
                    Text(text = "Record")
                }

                Button(onClick = {
                    startActivity(Intent(this@MainActivity, CameraActivity::class.java))
                }) {
                    Text(text = "Camera")
                }
                Button(onClick = {
                    startActivity(Intent(this@MainActivity, ProjectionActivity::class.java))
                }) {
                    Text(text = "Projection")
                }
                Button(onClick = {
                    startActivity(Intent(this@MainActivity, OldPlayActivity::class.java))
                }) {
                    Text(text = "VideoStream")
                }
                Button(onClick = {
                    doLikeMediaCodec()
                }) {
                    Text(text = "LikeMediaCodec")
                }
                Button(onClick = {
                    startActivity(Intent(this@MainActivity, VideoClipActivity::class.java))
                }) {
                    Text(text = "Clip")
                }
            }

        }
    }

    private fun doLikeMediaCodec() {
        val apkFile = File(Environment.getExternalStorageDirectory(), "out.h264")
        val likeMediaCodec = LikeMediaCodec(apkFile.absolutePath)
        likeMediaCodec.startCodec()
    }


    private fun checkPermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
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
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            builder.show()
        }
        return false
    }


}


/*@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    JormunPlaygroundAppTheme {
        Greeting("Android")
    }
}*/





