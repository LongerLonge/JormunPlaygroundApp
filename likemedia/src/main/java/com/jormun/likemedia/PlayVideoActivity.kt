package com.jormun.likemedia

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.jormun.likemedia.codec.H264Codec
import com.jormun.likemedia.utils.UiUtils
import java.io.File

class PlayVideoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaySurfaceView(UiUtils.dpToPx(368f), UiUtils.dpToPx(384f), MySurfaceCallback())
        }
    }
}

@Composable
fun PlaySurfaceView(width: Int, height: Int, sfCallback: SurfaceHolder.Callback) {
    ComposableSurfaceView(width, height, sfCallback)
}

@Composable
fun ComposableSurfaceView(width: Int, height: Int, sfCallback: SurfaceHolder.Callback) {
    AndroidView(factory = { context ->
        SurfaceView(context).apply {
            /*layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )*/
            layoutParams = ViewGroup.LayoutParams(
                width, height
            )
            holder.addCallback(sfCallback)
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
        /*_canvas = holder.lockCanvas()
        _canvas?.drawColor(Color.WHITE)
        _canvas?.drawCircle(100f, 100f, 50f, Paint().apply {
            color = Color.RED
        })
        holder.unlockCanvasAndPost(_canvas)*/
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

}



