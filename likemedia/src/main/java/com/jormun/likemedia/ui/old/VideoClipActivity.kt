package com.jormun.likemedia.ui.old

import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.VideoView
import com.jormun.likemedia.R
import com.jormun.likemedia.codec.MusicProcess
import com.jormun.likemedia.databinding.ActivityVideoClipBinding
import com.jormun.likemedia.ui.view.LarRangeSeekBar
import com.jormun.likemedia.ui.view.LarRangeSeekBar.LarRangeSeekBarListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class VideoClipActivity : AppCompatActivity() {
    private val TAG = "VideoClipActivity"

    private lateinit var videoView: VideoView
    private lateinit var rangeSeekBar: LarRangeSeekBar
    private lateinit var musicSeekBar: SeekBar
    private lateinit var voiceSeekBar: SeekBar
    private lateinit var loopCoo: Job
    private var isLooping = false

    //0和100是进度，*1000是因为视频播放的seek设置为毫秒，所以要相乘。
    private var videoStartSeek = 0 * 1000
    private var videoEndSeek = 100 * 1000

    //音量，0-2f
    private var musicVolume = 0.5f
    private var voiceVolume = 0.5f

    //
    private lateinit var videoFilePath: String
    private lateinit var audioFilePath: String

    //seekbar的监听类
    private val larRangeSeekBarListener = object : LarRangeSeekBarListener {
        override fun onCreate(rangeSeekBar: LarRangeSeekBar?, index: Int, value: Float) {
            Log.e(
                TAG,
                "RangeSeekBar " + rangeSeekBar!!.getId() + " - Created " + index + " " + value
            )
            //默认初始化的时候，设置为0和100
            rangeSeekBar.getThumbAt(0).value = 0f
            rangeSeekBar.getThumbAt(1).value = 100f
        }

        override fun onSeek(rangeSeekBar: LarRangeSeekBar?, index: Int, value: Float) {
            val id = rangeSeekBar!!.id
            Log.e(
                TAG,
                "RangeSeekBar " + rangeSeekBar.id + " - onSeek " + index + " " + value
            )
            if (index == 0) {
                videoStartSeek = value.toInt()
                videoView.seekTo(videoStartSeek)
            } else if (index == 1) {
                videoEndSeek = value.toInt()
            }
        }

        override fun onSeekStart(rangeSeekBar: LarRangeSeekBar?, index: Int, value: Float) {
            Log.e(
                TAG,
                "RangeSeekBar " + rangeSeekBar!!.getId() + " - onSeekStart " + index + " " + value
            )
        }

        override fun onSeekStop(rangeSeekBar: LarRangeSeekBar?, index: Int, value: Float) {
            Log.e(
                TAG,
                "RangeSeekBar " + rangeSeekBar!!.getId() + " - onSeekStop " + index + " " + value
            )
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_clip)
        videoView = findViewById(R.id.videoView)
        rangeSeekBar = findViewById(R.id.rangeSeekBar)
        rangeSeekBar.setListener(larRangeSeekBarListener)
        musicSeekBar = findViewById(R.id.musicSeekBar)
        voiceSeekBar = findViewById(R.id.voiceSeekBar)
        musicSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                musicVolume = progress / 100f
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
        voiceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                voiceVolume = progress / 100f
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
    }

    fun loadVideoAndAudio(view: View) {
        val videoFile = File(Environment.getExternalStorageDirectory(), "input3.mp4")
        videoFilePath = videoFile.absolutePath
        val musicFile = File(Environment.getExternalStorageDirectory(), "clipmusic.mp3")
        audioFilePath = musicFile.absolutePath
        val layoutParams = videoView.layoutParams
        /*layoutParams.width = 675
        layoutParams.height = 1285*/
        videoView.layoutParams = layoutParams
        videoView.setVideoPath(videoFile.absolutePath)
        videoView.start()
        videoView.setOnPreparedListener(object : OnPreparedListener {
            override fun onPrepared(mp: MediaPlayer?) {
                Log.e(TAG, "onPrepared: 准备了一次")
                videoView.seekTo(videoStartSeek)

                mp?.apply {
                    //isLooping = true
                    //设置下方进度条的最大值，并不是从0-100而是从0-视频长度ms
                    videoEndSeek = duration
                    rangeSeekBar.scaleRangeMax = duration.toFloat()
                }
                startLoopingCoo()
            }
        })
    }

    /**
     * 控制视频循环播放位置
     */
    private fun startLoopingCoo() {
        //死循环，每1000ms检查一次播放位置，超出规定范围就强行回退到开始位置
        //非常暴力且简单的循环播放实现方式
        if (isLooping) return
        loopCoo = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                if (videoView.currentPosition >= videoEndSeek) {
                    videoView.seekTo(videoStartSeek)
                    if (!videoView.isPlaying) {
                        videoView.start()
                    }
                }
                delay(1000)
            }
        }
    }

    fun music(view: View) {
        CoroutineScope(Dispatchers.IO).launch {
            val outputFile = File(Environment.getExternalStorageDirectory(), "cliptest.mp4")
            MusicProcess.mixAudioTrack(
                videoFilePath,
                audioFilePath,
                outputFile.absolutePath,
                cacheDir.absolutePath,
                videoStartSeek * 1000,
                videoEndSeek * 1000,
                voiceVolume * 2,
                musicVolume * 2
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loopCoo.cancel()
    }
}