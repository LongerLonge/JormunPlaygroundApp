package com.jormun.likemedia.ui.old

import android.graphics.Canvas
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import com.jormun.likemedia.R
import com.jormun.likemedia.codec.AudioRecordEncoder
import com.jormun.likemedia.codec.H264Codec
import com.jormun.likemedia.cons.MediaCodeType
import com.jormun.likemedia.net.SocketCallback
import com.jormun.likemedia.net.SocketLiveClient
import com.jormun.likemedia.net.SocketLivePush
import com.jormun.likemedia.ui.view.LocalSurfaceView
import com.jormun.likemedia.utils.VideoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class OldPlayActivity : AppCompatActivity(), View.OnClickListener, SocketCallback {

    private lateinit var socketLiveClient: SocketLiveClient
    private lateinit var h264Codec: H264Codec
    private lateinit var localSurfaceView: LocalSurfaceView
    private lateinit var audioRecordEncoder: AudioRecordEncoder
    private lateinit var socketLivePush: SocketLivePush

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_old_play)
        localSurfaceView = findViewById<LocalSurfaceView>(R.id.sf_old)
        val surfaceView = findViewById<SurfaceView>(R.id.sf_out)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                //创建视频解码器
                h264Codec = H264Codec(surface = holder.surface, isStream = true)
                //创建音频解码器
                audioRecordEncoder = AudioRecordEncoder()

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

        findViewById<Button>(R.id.btn_start_push).setOnClickListener(this)
        findViewById<Button>(R.id.btn_link_xm).setOnClickListener(this)
        findViewById<Button>(R.id.btn_link_samsung).setOnClickListener(this)
    }


    private fun loadH264File(): String {
        val apkFile = File(Environment.getExternalStorageDirectory(), "out.h264")
        return apkFile.absolutePath
    }

    override fun onClick(v: View?) {
        v?.apply {
            when (id) {
                R.id.btn_start_push -> {
                    //开始推送，创建Socket
                    socketLivePush = SocketLivePush()
                    //给相机数据捕获端设置好推送Socket
                    localSurfaceView.setSocketLivePush(socketLivePush)
                    localSurfaceView.isStream = true
                    //音频端也一样设置好推送Socket
                    audioRecordEncoder.setUpSocketLivePush(socketLivePush)
                    //音频开始录制
                    audioRecordEncoder.startRecord(this@OldPlayActivity)
                    //视频解码和音频解码互相绑定
                    h264Codec.setAudioEncoder(audioRecordEncoder)
                }

                R.id.btn_link_xm -> {
                    //socketLiveClient = SocketLiveClient(h264Codec, "192.168.31.60", 9007)
                    socketLiveClient = SocketLiveClient(this@OldPlayActivity, "192.168.31.60", 9007)
                    socketLiveClient.start()
                    //h264Codec.play()
                }

                R.id.btn_link_samsung -> {
                    //socketLiveClient = SocketLiveClient(h264Codec, "192.168.31.120", 9007)
                    socketLiveClient =
                        SocketLiveClient(this@OldPlayActivity, "192.168.31.120", 9007)
                    socketLiveClient.start()
                    //h264Codec.play()
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        socketLivePush.socketClose()
        socketLiveClient.socketClose()
    }

    /**
     * WebSocket接收的回传数据会回调此处。
     */
    override fun callback(data: ByteArray) {
        //解析数据类型
        val dataType = VideoUtils.encodeDataType(data)
        //说明是视频
        MediaCodeType.VIDEO_DATA.apply {
            if (dataType.containsKey(this))
                h264Codec.decodeVideoData(dataType[this]!!)
        }
        //说明是音频
        MediaCodeType.AUDIO_DATA.apply {
            if (dataType.containsKey(this))
                audioRecordEncoder.doPlay(dataType[this]!!)
        }
    }


}