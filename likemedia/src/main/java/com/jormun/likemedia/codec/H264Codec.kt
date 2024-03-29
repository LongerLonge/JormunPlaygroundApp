package com.jormun.likemedia.codec

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.jormun.likemedia.cons.MediaCodeType
import com.jormun.likemedia.cons.VideoEncodeFormat
import com.jormun.likemedia.net.SocketCallback
import com.jormun.likemedia.utils.VideoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream

/**
 * 负责把H264数据解码进行播放的工具类。
 * @param path: 需要解码播放的文件，.h264结尾，如果是视频流就不需要。
 * @param surface: 需要播放到的SurfaceView。
 * @param isStream: 标记是否为视频流
 */
class H264Codec(
    private val path: String = "",
    private val surface: Surface,
    private val isStream: Boolean = false
) : SocketCallback {

    private val TAG: String = "H264Codec"

    private lateinit var mediaCodec: MediaCodec
    private lateinit var audioRecordEncoder: AudioRecordEncoder

    fun play() {
        if (!isStream) decodeH264(path)
    }

    private fun initMediaCodec() {
        try {
            mediaCodec = MediaCodec.createDecoderByType(VideoEncodeFormat.VIDEO_MIMETYPE)
            val videoFormat = MediaFormat.createVideoFormat(
                VideoEncodeFormat.VIDEO_MIMETYPE,
                VideoEncodeFormat.VIDEO_WIDTH,
                VideoEncodeFormat.VIDEO_HEIGHT
            )
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20)
            if (isStream) {
                videoFormat.setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    VideoEncodeFormat.VIDEO_WIDTH * VideoEncodeFormat.VIDEO_HEIGHT
                )
                videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            mediaCodec.configure(videoFormat, surface, null, 0)
        } catch (e: Exception) {
            Log.e(TAG, "init codec err : ${e.printStackTrace()}")
        }
    }

    private fun decodeH264(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (!this@H264Codec::mediaCodec.isInitialized) {
                initMediaCodec()
                mediaCodec.start()
            }
            try {
                //先把h264文件转成字节数组
                val h264Bytes = getH264Bytes(path)
                //获得整个容器队列，可以实现但是不推荐
                //val inputBuffers = mediaCodec.getInputBuffers()
                //一帧的起始下标
                var startIndex = 0
                //解码后的buffer信息需要一个对象来接收
                val bufferInfo = MediaCodec.BufferInfo()
                while (true) {
                    //delay(33)
                    //找到下一帧的开始位置，同时也是当前一帧的结束位置
                    val nextFrameIndex =
                        VideoUtils.findNextFrame(h264Bytes, startIndex + 2, h264Bytes.size)
                    if (nextFrameIndex == -1) break
                    //尝试等待，获得可用容器的下标
                    val idleIndex = mediaCodec.dequeueInputBuffer(10000)
                    //不为0，则说明获取可用容器下标成功
                    while (idleIndex >= 0) {//内循环直到所有数据都取出完毕为止
                        //取出可用容器，推荐使用这种方式
                        val byteBuffer = mediaCodec.getInputBuffer(idleIndex)
                        val len = nextFrameIndex - startIndex
                        byteBuffer?.apply {
                            //把一帧的数据放入容器中
                            put(h264Bytes, startIndex, len)
                            //告诉dps我们放置这一帧数据到哪个容器。
                            //idleIndex：使用的容器下标、offset：起始位置、size：大小、pts：是否按照sps也就是时间戳解码0为是
                            mediaCodec.queueInputBuffer(idleIndex, 0, len, 0, 0)
                            //移动起始点
                            startIndex = nextFrameIndex
                            //等待1秒钟，从dps中尝试获取已经解码好的数据容器下标
                            val completeBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                            //不为0，则说明获取容器下标成功
                            if (completeBufferIndex >= 0) {
                                val newFormat: MediaFormat = mediaCodec.outputFormat
                                //Log.e(TAG, "decodeH264: w: ${ newFormat.getInteger("width")} h: ${newFormat.getInteger("height")}")
                                //直接指挥Codec把解码好的下标容器里的数据渲染到surface中
                                mediaCodec.releaseOutputBuffer(completeBufferIndex, true)
                                // break
                            } else {
                                Log.e(TAG, "decodeH264 err: return${completeBufferIndex}")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "decodeH264 err: ${e.message}")
                e.printStackTrace()
            }
        }
    }


    /**
     * 把h264文件转成字节数组
     */
    private fun getH264Bytes(path: String): ByteArray {
        val input = DataInputStream(FileInputStream(File(path)))
        var len = 0
        val size = 1024
        var buff = ByteArray(size)
        val bos = ByteArrayOutputStream()
        while (true) {
            len = input.read(buff, 0, size)
            if (len == -1) break
            bos.write(buff, 0, len)
        }
        buff = bos.toByteArray()
        return buff
    }

    fun setAudioEncoder(audioRecordEncoder: AudioRecordEncoder) {
        this.audioRecordEncoder = audioRecordEncoder
    }


    /**
     * 视频流回调处，如果是网络传输的在线视频流就走这里。
     * 目前只是在投屏功能中作学习演示用。
     */
    override fun callback(data: ByteArray) {
        Log.e(TAG, "callback: 收到数据开始解码")
        synchronized(H264Codec::class.java) {
            //先解析标记位看下是视频还是音频数据
            val dataType = VideoUtils.encodeDataType(data)
            //
            if (dataType.containsKey(MediaCodeType.VIDEO_DATA)) {//说明是视频
                decodeVideoData(dataType[MediaCodeType.VIDEO_DATA]!!)
            }
        }
    }


    /**
     * 处理视频数据
     */
    fun decodeVideoData(data: ByteArray) {
        //Log.e(TAG, "decodeVideoData: 视频数据")
        if (!this@H264Codec::mediaCodec.isInitialized) {
            initMediaCodec()
            mediaCodec.start()
        }
        Log.i(TAG, "解码器前长度  : " + data.size)
        //第一步，先塞数据给dsp容器，让其帮忙解码。
        val inputIndex = mediaCodec.dequeueInputBuffer(100000)
        if (inputIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(inputIndex)
            inputBuffer?.apply {
                clear()
                inputBuffer.put(data, 0, data.size)
                mediaCodec.queueInputBuffer(
                    inputIndex, 0, data.size, System.currentTimeMillis(), 0
                )
            }
        }
        //第二步，查询dsp的容器，有的话就从容器中取出已经解码好的数据。
        val bufferInfo = MediaCodec.BufferInfo()

        var outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000)
        Log.i(TAG, "解码器后长度  : " + bufferInfo.size)

        if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            val outputFormat = mediaCodec.getOutputFormat()
            val width = outputFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = outputFormat.getInteger(MediaFormat.KEY_HEIGHT)
            Log.e(TAG, "解码后获得的宽高: 宽：${width} 高：${height}")
        }

        //while循环不断的查询并取出容器中的已解码数据，并渲染到SurfaceView中去。
        while (outputIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outputIndex, true)
            val mediaExtractor = MediaExtractor()

            outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
        }
    }


}