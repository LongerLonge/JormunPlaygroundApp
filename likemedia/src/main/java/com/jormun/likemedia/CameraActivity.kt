package com.jormun.likemedia

import android.Manifest
import android.content.Context

import android.content.pm.PackageManager
import android.graphics.Canvas

import android.os.Bundle
import android.view.SurfaceHolder

import android.view.ViewGroup

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column



import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jormun.likemedia.codec.H264Codec
import com.jormun.likemedia.codec.H264RecordCodec
import com.jormun.likemedia.net.SocketLiveClient

import com.jormun.likemedia.ui.view.LocalSurfaceView
import com.jormun.likemedia.utils.UiUtils

class CameraActivity : ComponentActivity() {
    private val TAG = "CameraActivity"
    private lateinit var cameraSurfaceView: LocalSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
        //Log.e(TAG, "onCreate: load h264 file success: ${loadH264File()}")
        setContent {
            Column(
            ) {
                cameraSurfaceView = cameraSurfaceView(modifier = Modifier.weight(1f))
                Button(onClick = {
                    cameraSurfaceView.isStream = true
                }) {
                    Text(text = "开始传输画面")
                }
            }
        /*Box(modifier = Modifier.fillMaxSize()) {

            Column(
            ) {
                cameraSurfaceView = cameraSurfaceView(modifier = Modifier.weight(1f))
                Button(onClick = {
                    cameraSurfaceView.isStream = true
                },modifier = Modifier.weight(1f)) {
                    Text(text = "开始传输画面")
                }
            }
            Column(
            modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                PlaySurfaceView(
                    UiUtils.dpToPx(100f),
                    UiUtils.dpToPx(200f),
                    ComSurfaceCallback()
                )
                Button(onClick = {

                }) {
                    Text(text = "开始传输画面")
                }
            }
        }*/



        }
    }

    private fun checkPermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }
        return false
    }

}

@Preview
@Composable
fun cameraSurfaceView(modifier: Modifier = Modifier): LocalSurfaceView {
    val context = LocalContext.current
    val lfv = rememberLocalSurfaceView(context)
    ComposableCameraSurfaceView(lfv)
    return lfv
}

@Composable
fun ComposableCameraSurfaceView(localSurfaceView: LocalSurfaceView) {
    AndroidView(factory = { context ->
        localSurfaceView
    })
}

@Composable
fun rememberLocalSurfaceView(context: Context): LocalSurfaceView {
    //自定义的一个SurfaceView，负责打开摄像头并录制
    val lfv = remember {
        LocalSurfaceView(context = context)
    }
    lfv.apply {
        layoutParams = ViewGroup.LayoutParams(
            UiUtils.dpToPx(context, 393f), UiUtils.dpToPx(context, 699f)
        )
    }
    return lfv
}

class ComSurfaceCallback : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
        // TODO: 接收数据播放到SurfaceView
        /*val h264Codec = H264Codec(surface = holder.surface, isStream = true)
        val socketLiveClient = SocketLiveClient(h264Codec, 9007)
        socketLiveClient.start()*/
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

}






