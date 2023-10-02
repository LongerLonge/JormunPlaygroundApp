package com.jormun.likemedia.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.jormun.likemedia.cons.VideoFormat
import com.jormun.likemedia.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class H264RecordCodec(private var width: Int, private var height: Int) {

    private lateinit var mediaCodec: MediaCodec

    private var index = 0

    private val TAG = "H264RecordCodec"


    fun startLive() {
        try {
            mediaCodec = MediaCodec.createEncoderByType(VideoFormat.VIDEO_MIMETYPE)
            val format = MediaFormat.createVideoFormat(VideoFormat.VIDEO_MIMETYPE, width, height)
            //码率
            format.setInteger(MediaFormat.KEY_BIT_RATE, width * height)
            //1s多少帧
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
            //I帧间隔
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            //因为是摄像头传入的数据，所以这里设置色彩空间类型为yuv420
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun encodeFrame(buffer: ByteArray?): Int {
        if (buffer == null) return -1

        val inputBufferIndex = mediaCodec.dequeueInputBuffer(10000)
        val bufferInfo = MediaCodec.BufferInfo()
        if (inputBufferIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
            inputBuffer?.apply {
                clear()
                put(buffer)
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, buffer.size, computePts(index), 0)
                index++

            }
        }

        val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000)
        if (outputBufferIndex >= 0) {
            val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
            val data = ByteArray(bufferInfo.size)
            outputBuffer?.apply {
                get(data)
                Log.e(TAG, "encodeFrame: 开始写入文件？")
                writeToFile(data)
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
            }
        }

        return -1
    }

    private fun writeToFile(data: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            FileUtils.writeBytes(data, "codec.h264")
            FileUtils.writeContent(data, "codecH264")
        }
    }

    private fun computePts(index: Int): Long {
        return 1000000L / 15 * index
    }

}