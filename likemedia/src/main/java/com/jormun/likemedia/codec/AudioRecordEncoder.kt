package com.jormun.likemedia.codec

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.jormun.likemedia.cons.AudioEncodeFormat
import com.jormun.likemedia.cons.MediaCodeType
import com.jormun.likemedia.net.SocketLivePush
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream


/**
 * 负责音频的编码工作。
 */
class AudioRecordEncoder {

    private var isRecording = false
    private lateinit var audioData: ByteArray
    private var isStream = false
    private lateinit var socketLivePush: SocketLivePush
    private lateinit var audioTrack: AudioTrack

    fun setUpSocketLivePush(socketLivePush: SocketLivePush) {
        this.socketLivePush = socketLivePush
        isStream = true
    }

    /**
     * 开启音频的录制
     */
    fun startRecord(context: Context) {
        if (isRecording) return
        synchronized(AudioRecordEncoder::class.java) {
            //先算出数据长度，就是采样率*通道数*采样位数而已。
            val minBufferSize = AudioRecord.getMinBufferSize(
                AudioEncodeFormat.SAMPLE_RATE_INHZ,//采样率
                AudioEncodeFormat.CHANNEL_CONFIG,//通道数
                AudioEncodeFormat.AUDIO_FORMAT_BIT//采样位数
            )
            if (!this@AudioRecordEncoder::audioData.isInitialized || !isRecording)
                audioData = ByteArray(minBufferSize)

            //录音之前需要先检查权限
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            //权限没问题后，可以开始录制
            //新建录制类
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,//录音来源，mic就是麦克风
                AudioEncodeFormat.SAMPLE_RATE_INHZ,//采样率
                AudioEncodeFormat.CHANNEL_CONFIG,//通道数
                AudioEncodeFormat.AUDIO_FORMAT_BIT,//采样位数
                minBufferSize//数据长度
            )
            //初始化播放器：
            initPlay()
            //开始录制
            audioRecord.startRecording()
            //子线程循环获取数据
            cooStartRecording(audioRecord, minBufferSize)
            isRecording = true
        }
    }

    /**
     * 协程丢到IO里面循环获取录制的数据
     */
    private fun cooStartRecording(audioRecord: AudioRecord, minBufferSize: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            while (isRecording) {
                val len = audioRecord.read(audioData, 0, minBufferSize)
                if (isStream)
                    socketSend(audioData)//如果是流就直接推出去。
                else {
                    /* FileUtils.writeBytes(audioData, "audiotest.pcm")
                 FileUtils.writeBytes(audioData, "audiotestPcm")*/
                }
            }
        }
    }

    /**
     * 利用Socket发送码流数据。
     * @param byteArray: 已经解析好的码流数据。
     */
    private fun socketSend(byteArray: ByteArray) {
        if (this::socketLivePush.isInitialized) {
            socketLivePush.sendData(byteArray, MediaCodeType.AUDIO_DATA)
        } else throw Exception("socketLive is not initialized!!")
    }

    /**
     * 初始化音频播放器，用来播放网络上收到的数据。
     */
    private fun initPlay() {
        Log.i("Tag8", "go there")
        //配置播放器
        //音乐类型,扬声器播放
        val streamType = AudioManager.STREAM_MUSIC
        //录音时采用的采样频率,所有播放时同样的采样频率
        val sampleRate = AudioEncodeFormat.SAMPLE_RATE_INHZ
        //单声道,和录音时设置的一样
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        // 录音使用16bit,所有播放时同样采用该方式
        val audioFormat = AudioEncodeFormat.AUDIO_FORMAT_BIT
        //流模式
        val mode = AudioTrack.MODE_STREAM
        //计算最小buffer大小
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        //构造AudioTrack  不能小于AudioTrack的最低要求，也不能小于我们每次读的大小
        audioTrack = AudioTrack(
            streamType, sampleRate, channelConfig, audioFormat,
            minBufferSize, mode
        )
        //默认音量
        audioTrack.setVolume(16f)
        //从文件流读数据
        val inputStream: FileInputStream? = null
        audioTrack.play()
    }

    /**
     * 解析传入的音频数据并播放。
     */
    fun doPlay(mBuffer: ByteArray) {
        val ret = audioTrack.write(mBuffer, 0, mBuffer.size)
        Log.i("Tag8", "ret ===$ret")
        when (ret) {
            AudioTrack.ERROR_INVALID_OPERATION, AudioTrack.ERROR_BAD_VALUE, AudioManager.ERROR_DEAD_OBJECT -> return
            else -> {}
        }
        Log.i("Tag8", "播放成功。。。。")
    }
}