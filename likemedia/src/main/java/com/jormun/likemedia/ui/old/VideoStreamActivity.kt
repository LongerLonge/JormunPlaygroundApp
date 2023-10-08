package com.jormun.likemedia.ui.old

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import com.jormun.likemedia.R
import com.jormun.likemedia.codec.H264Codec
import com.jormun.likemedia.databinding.ActivityVideoStreamBinding
import com.jormun.likemedia.net.SocketLiveClient
import com.jormun.likemedia.ui.view.LocalSurfaceView

class VideoStreamActivity : AppCompatActivity(), View.OnClickListener {

    //To add
    lateinit var binding: ActivityVideoStreamBinding

    //private lateinit var lsf_in: LocalSurfaceView
    //private lateinit var sf_out: SurfaceView
    private lateinit var btn_start_push: Button
    private lateinit var btn_link_xm: Button
    private lateinit var btn_link_samsung: Button
    private lateinit var socketLiveClient: SocketLiveClient
    private lateinit var h264Codec: H264Codec
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=  ActivityVideoStreamBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_video_stream)
        binding.btnStartPush.setOnClickListener(this)
        binding.btnLinkXm.setOnClickListener(this)
        binding.btnLinkSamsung.setOnClickListener(this)

        initSurfaceView()
    }

    override fun onStart() {
        super.onStart()

    }

    private fun initSurfaceView() {
        binding.sfOut.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // TODO: 接收数据播放到SurfaceView
                h264Codec = H264Codec(surface = holder.surface, isStream = true)
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


    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_start_push -> {
                binding.lfIn.isStream = true
            }

            R.id.btn_link_samsung -> {
                socketLiveClient = SocketLiveClient(h264Codec, "192.168.31.120", 9007)
                socketLiveClient.start()
                h264Codec.play()
            }

            R.id.btn_link_xm -> {
                socketLiveClient = SocketLiveClient(h264Codec, "192.168.31.60", 9007)
                socketLiveClient.start()
                h264Codec.play()
            }
        }
    }


}