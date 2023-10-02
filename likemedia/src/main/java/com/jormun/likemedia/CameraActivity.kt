package com.jormun.likemedia

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.jormun.likemedia.codec.H264Codec
import com.jormun.likemedia.ui.theme.JormunPlaygroundAppTheme
import com.jormun.likemedia.ui.view.LocalSurfaceView
import java.io.File

class CameraActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
        //Log.e(TAG, "onCreate: load h264 file success: ${loadH264File()}")
        setContent {
            Column {
                MainLocalSurfaceView()
            }

        }
    }

    private fun checkPermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                ), 1
            )
        }

        return false
    }

}

@Preview
@Composable
fun MainLocalSurfaceView() {
    ComposableLocalSurfaceView()
}

@Composable
fun ComposableLocalSurfaceView(modifier: Modifier = Modifier) {
    AndroidView(factory = { context ->
        LocalSurfaceView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                1000,
                2000
            )
        }
    })
}






