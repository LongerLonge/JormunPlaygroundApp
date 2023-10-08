package com.jormun.likemedia.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.jormun.likemedia.cons.MediaCodeType
import com.jormun.likemedia.cons.VideoEncodeFormat
import com.jormun.likemedia.net.SocketLivePush
import com.jormun.likemedia.utils.FileUtils
import com.jormun.likemedia.utils.VideoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 摄像头采集数据然后给这里编码
 * @param isStream: 是否需要直接推送到websocket
 * @param codeType: 编码类型
 */
class H264RecordCodec(
    private var width: Int = VideoEncodeFormat.VIDEO_WIDTH,
    private var height: Int = VideoEncodeFormat.VIDEO_HEIGHT,
    private var isStream: Boolean = false,
    private var codeType: CodeType = CodeType.H264
) : BaseEncoder {

    private lateinit var mediaCodec: MediaCodec

    private var index = 0

    private val TAG = "H264RecordCodec"

    private lateinit var socketLivePush: SocketLivePush

    private lateinit var sps_pps_buf: ByteArray

    private lateinit var nv12: ByteArray
    private lateinit var yuv: ByteArray

    fun setTheSocketLive(socketLivePush: SocketLivePush) {
        this.socketLivePush = socketLivePush
        if (!isStream) isStream = true
    }

    fun getTheSocketLive(): SocketLivePush {
        return socketLivePush
    }

    override fun startEncoder() {
        if (this::mediaCodec.isInitialized) return
        try {
            mediaCodec = MediaCodec.createEncoderByType(VideoEncodeFormat.VIDEO_MIMETYPE)
            //宽高互换，因为摄像头默认就是旋转的。
            val format =
                MediaFormat.createVideoFormat(VideoEncodeFormat.VIDEO_MIMETYPE, height, width)
            //码率
            format.setInteger(
                MediaFormat.KEY_BIT_RATE,
                VideoEncodeFormat.VIDEO_WIDTH * VideoEncodeFormat.VIDEO_HEIGHT
            )
            //1s多少帧
            format.setInteger(MediaFormat.KEY_FRAME_RATE, VideoEncodeFormat.FRAME_RATE)
            //I帧间隔
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VideoEncodeFormat.I_FRAME_INTERVAL)
            //因为是摄像头传入的数据，所以这里设置色彩空间类型为yuv420
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec.start()
            //构建YUV容器数组，YUV大小为1.5字节乘以宽高
            val bufferLength = width * height * 3 / 2
            yuv = ByteArray(bufferLength)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun dealSpsFrameAndSave(frameBuffer: ByteArray) {

    }

    /**
     * 摄像头采集到的每一帧数据都会传输到这里处理
     */
    fun encodeFrame(buffer: ByteArray?) {
        try {
            if (buffer == null) throw Exception("camera buffer is null!")
            //把老旧的NV21转换成NV12
            nv12 = VideoUtils.nv21toNV12(buffer)
            //把NV12旋转90°使其成为正常画面
            VideoUtils.portraitData2Raw(nv12, yuv, width, height)

            val inputBufferIndex = mediaCodec.dequeueInputBuffer(100000)

            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                inputBuffer?.apply {
                    clear()
                    put(yuv)
                    mediaCodec.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        yuv.size,
                        computePts(index),
                        0
                    )
                    index++
                }
            }
            val bufferInfo = MediaCodec.BufferInfo()
            val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000)
            if (outputBufferIndex >= 0) {
                val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                val data = ByteArray(bufferInfo.size)
                outputBuffer?.apply {
                    //需要先保存sps和pps
                    if (!this@H264RecordCodec::sps_pps_buf.isInitialized) {
                        sps_pps_buf = VideoUtils.dealSpsPpsFrame(outputBuffer, bufferInfo, codeType)
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                        return
                    }
                    if (!isStream) {
                        /*get(data)
                        Log.e(TAG, "encodeFrame: 开始写入文件？")
                        writeToFile(data)*/
                    } else {
                        val dealFrame =
                            VideoUtils.dealFrameWithSps(
                                sps_pps_buf,
                                outputBuffer,
                                bufferInfo,
                                codeType
                            )
                        socketSend(dealFrame)
                    }
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeToFile(data: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            FileUtils.writeBytes(data, "codec.h264")
            FileUtils.writeContent(data, "codecH264")
        }
    }

    /**
     * 计算帧间隔时间
     */
    private fun computePts(index: Int): Long {
        //因为我们假设是1s 15帧，所以算法很简单的就是把1s切割成15份即可，然后根据index来获取是这15份里面的第几个。
        return 1000000L / 15 * index
    }


    /**
     * 利用Socket发送码流数据。
     * @param byteArray: 已经解析好的码流数据。
     */
    private fun socketSend(byteArray: ByteArray) {
        //startStream()
        if (this::socketLivePush.isInitialized) {
            socketLivePush.sendData(byteArray, MediaCodeType.VIDEO_DATA)
            //Log.e(TAG, "sendFrame: ${byteArray.contentToString()}")
        } else throw Exception("socketLive is not initialized!!")
    }
}