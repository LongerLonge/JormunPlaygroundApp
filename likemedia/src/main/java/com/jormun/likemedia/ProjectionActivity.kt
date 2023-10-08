package com.jormun.likemedia

import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.jormun.likemedia.codec.H264Codec
import com.jormun.likemedia.codec.H264Encoder
import com.jormun.likemedia.net.SocketLiveClient
import com.jormun.likemedia.ui.theme.JormunPlaygroundAppTheme
import com.jormun.likemedia.utils.UiUtils
import java.io.File

class ProjectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjectionSurfaceView()
        }
    }
}


@Preview
@Composable
fun ProjectionSurfaceView() {
    ComposableProjectionSurfaceView()
}

@Composable
fun ComposableProjectionSurfaceView(modifier: Modifier = Modifier) {
    AndroidView(factory = { context ->


        SurfaceView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            /*layoutParams = ViewGroup.LayoutParams(
                UiUtils.dpToPx(context,393f),
                UiUtils.dpToPx(context,699f)
            )*/
            holder.addCallback(ProjectionSurfaceCallback())
        }
    })
}

class ProjectionSurfaceCallback : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
        // TODO: 接收数据播放到SurfaceView
        val h264Codec = H264Codec(surface = holder.surface, isStream = true)
        val socketLiveClient = SocketLiveClient(h264Codec, port = 9007)
        socketLiveClient.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.e("ProjectionSurfaceCallback", "surfaceChanged: width:${width} height:${height}")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

}

