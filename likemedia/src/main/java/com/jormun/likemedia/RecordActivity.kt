package com.jormun.likemedia

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.jormun.likemedia.codec.CodeType
import com.jormun.likemedia.codec.H264Encoder
import com.jormun.likemedia.cons.VideoFormat
import com.jormun.likemedia.net.SocketLivePush
import com.jormun.likemedia.utils.UiUtils


class RecordActivity : ComponentActivity() {

    private val TAG = "RecordActivity"
    private var isCanRecord = false
    private var mBound: Boolean = false

    private lateinit var mService: ScreenRecordService




    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as ScreenRecordService.LocalBinder
            mService = binder.getService()
            mBound = true
            isCanRecord = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                Button(onClick = {
                    startRecord(false)
                }) {
                    Text(text = "开始本地录屏")
                }

                Button(onClick = {
                    startRecord(true)
                }) {
                    Text(text = "开始投屏")
                }

                Button(onClick = {
                    Toast.makeText(this@RecordActivity, "视频是在动的！", Toast.LENGTH_SHORT).show()
                }) {
                    Text(text = "点击证明视频有动")
                }
            }

        }
        //
        getRecordPermission()
    }

    private fun startRecord(isStream: Boolean) {
        var text = "已经开启录屏，请点击下方按钮让画面动起来！"
        if (isCanRecord) {
            if (mBound)
                mService.startRecord(isStream, VideoFormat.CODE_TYPE)
            else {
                val h264Encoder = H264Encoder(mediaProjection, VideoFormat.VIDEO_WIDTH, VideoFormat.VIDEO_HEIGHT, isStream, VideoFormat.CODE_TYPE)
                if (isStream) {
                    val socketLivePush = SocketLivePush()
                    h264Encoder.setTheSocketLive(socketLivePush)
                    socketLivePush.start(h264Encoder)
                } else
                    h264Encoder.startEncoder()
            }
        } else {
            text = "没有权限不能录制。"
        }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection

    private fun getRecordPermission() {
        //这种方式只适用于28或以下的系统
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(screenCaptureIntent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || requestCode != 1) {
            Toast.makeText(this, "拒绝，无法录制。", Toast.LENGTH_SHORT).show()
            return
        }


        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                //高于28的需要用前台服务来开启录屏
                // Bind to ScreenShortRecordService
                if (!mBound) {
                    Intent(this, ScreenRecordService::class.java).also { intent ->
                        intent.putExtra("code", resultCode)
                        intent.putExtra("data", data)
                        bindService(intent, connection, Context.BIND_AUTO_CREATE)
                    }
                }
            } else {
                //低于28的直接就可以录屏
                data?.apply {
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                    isCanRecord = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        mBound = false
    }
}