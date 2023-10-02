package com.jormun.likemedia

import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
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
import com.jormun.likemedia.ui.theme.JormunPlaygroundAppTheme
import com.jormun.likemedia.utils.UiUtils
import java.io.File

class PlayVideoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainSurfaceView()
        }
    }
}

@Preview
@Composable
fun MainSurfaceView() {
    ComposableSurfaceView()
}

@Composable
fun ComposableSurfaceView(modifier: Modifier = Modifier) {
    AndroidView(factory = { context ->
        SurfaceView(context).apply {
            /*layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )*/
            layoutParams = ViewGroup.LayoutParams(
                UiUtils.dpToPx(context,368f),
                UiUtils.dpToPx(context,384f)
            )
            holder.addCallback(MySurfaceCallback())
        }
    })
}

private fun loadH264File(): String {
    val apkFile = File(Environment.getExternalStorageDirectory(), "out.h264")
    return apkFile.absolutePath
}

class MySurfaceCallback : SurfaceHolder.Callback {
    private var _canvas: Canvas? = null
    override fun surfaceCreated(holder: SurfaceHolder) {
        val h264Codec = H264Codec(loadH264File(), holder.surface)
        h264Codec.play()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

}



