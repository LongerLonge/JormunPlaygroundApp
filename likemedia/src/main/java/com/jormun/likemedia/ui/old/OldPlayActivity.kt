package com.jormun.likemedia.ui.old

import android.graphics.Canvas
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.jormun.likemedia.R
import com.jormun.likemedia.codec.H264Codec
import com.jormun.likemedia.net.SocketLiveClient
import java.io.File

class OldPlayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_old_play)
        val surfaceView = findViewById<SurfaceView>(R.id.sf_old)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                /*val h264Codec = H264Codec(loadH264File(), holder.surface)
                h264Codec.play()*/
                val h264Codec = H264Codec(surface = holder.surface, isStream = true)
                val socketLiveClient = SocketLiveClient(h264Codec, 9007)
                socketLiveClient.start()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }

        })
    }

    private fun loadH264File(): String {
        val apkFile = File(Environment.getExternalStorageDirectory(), "out.h264")
        return apkFile.absolutePath
    }


}