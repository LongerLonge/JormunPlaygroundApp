package com.jormun.likemedia.codec

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.util.Log
import com.jormun.likemedia.cons.VideoFormat
import com.jormun.likemedia.net.SocketLivePush
import com.jormun.likemedia.utils.FileUtils
import com.jormun.likemedia.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

/**
 * 负责把YUV数据编码成H264格式的工具类。
 */
class H264Encoder(
    private val mediaProjection: MediaProjection,
    private val width: Int = VideoFormat.VIDEO_WIDTH,
    private val height: Int = VideoFormat.VIDEO_HEIGHT,
    private val isStream: Boolean = false,
    private val codeType: CodeType
) {

    private val TAG = "H264Encoder"

    private lateinit var mediaCodec: MediaCodec

    private lateinit var virtualDisplay: VirtualDisplay

    private val NAL_VPS = 32
    private val NAL_265_SPS = 33
    private val NAL_265_IDR = 19
    private val NAL_264_SPS = 7
    private val NAL_264_IDR = 5

    private lateinit var sps_pps_buf: ByteArray

    private lateinit var socketLivePush: SocketLivePush


    init {
        initEncoderData()
    }

    fun setTheSocketLive(socketLivePush: SocketLivePush) {
        this.socketLivePush = socketLivePush
    }

    private fun initEncoderData() {
        //创建编码格式类
        val encoderFormat = MediaFormat.createVideoFormat(VideoFormat.VIDEO_MIMETYPE, width, height)

        try {
            //创建编码类型的MediaCodec
            mediaCodec = MediaCodec.createEncoderByType(VideoFormat.VIDEO_MIMETYPE)
            //设置编码帧率为20，就是1s -> 20帧，也可以理解为1s编码20帧(并不是20s编码一个I帧而是所有帧)
            encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20)
            //设置默认的I帧间隔为30s，也会视乎情况(如场景变化)，不一定。
            encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            //设置编码码率，越高质量越好，但是越占空间
            encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height)
            //设置编码色彩规格，可以理解为数据的来源
            encoderFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            mediaCodec.configure(encoderFormat, null, null, CONFIGURE_FLAG_ENCODE)
            //创建编码数据缓冲区
            val inputSurface = mediaCodec.createInputSurface()
            //把录屏对象的数据跟编码输入缓冲区绑定到一起，等于把录取的数据暂存到指定的位置
            //参数：name为绑定命名、宽高、dpi指的是一英寸像素密度越高越清晰、flag指是否公开、surface指的就是关键的数据缓冲区
            //callback指的是录屏状态回调、handler你传入去就会通过handler给你发一些消息
            //这里的宽高跟上面编码器的宽高最好一致，否则如果这里录制的数据过大编码器过小就会造成不必要的性能损失
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "jormun-encoder",
                width,
                height,
                3,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                inputSurface,
                null,
                null
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 开始编码
     */
    fun startEncoder() {
        CoroutineScope(Dispatchers.IO).launch {
            //编码跟解码不一样，编码应该上面已经对投影对象和编码对象进行了绑定，所以这里就不需要像解码一样要设置输入数据
            //编码对象会直接去绑定的缓冲区中取出录制数据进行编码。
            try {
                mediaCodec.start()
                //创建接收的Buffer信息
                val bufferInfo = MediaCodec.BufferInfo()
                while (true) {
                    //查看缓冲区中是否已经有编码好的数据
                    val outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
                    //不为0则说明有某个缓冲区容器已经有编码好的数据
                    if (outIndex >= 0) {
                        //直接把容器取出来
                        val outputBuffer = mediaCodec.getOutputBuffer(outIndex)
                        //创建一个字节数组，然后把容器中的数据装进去
                        val byteArray = ByteArray(bufferInfo.size)
                        outputBuffer?.apply {
                            if (!isStream) {
                                get(byteArray)
                                //把字节码写到文件
                                FileUtils.writeBytes(byteArray, "codec.h264")
                                //把字节码以十六进制的格式写入(h265只能解码十六进制)
                                FileUtils.writeContent(byteArray, "codecH264")
                            } else {
                                dealFrame(this, bufferInfo, codeType)
                            }
                        }
                        //释放这个容器
                        mediaCodec.releaseOutputBuffer(outIndex, false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 解析码流数据
     */
    private fun dealFrame(
        frameBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, codeType: CodeType
    ) {
        var mask = 0x1F
        var startType = NAL_264_SPS
        var iDRType = NAL_264_IDR
        if (codeType == CodeType.H265) {
            mask = 0x7E
            startType = NAL_VPS
            iDRType = NAL_265_IDR
        }

        var offset = 4//00 00 00 01
        if (frameBuffer.get(2).toInt() == 0x01) {//兼容00 00 01
            offset = 3
        }
        //算出分隔符后一位的type，利用&蒙版数可以求出来
        var frameType = frameBuffer.get(offset).toInt().and(mask)//and等同与&

        if (codeType == CodeType.H265) {//h265还要右移一位才行
            frameType = frameType.shr(1)//等同于>>1
        }

        if (frameType == startType) {//判断是否为SPS或者VPS
            //因为编码器只会输出一次这部分数据，需要保存起来。
            sps_pps_buf = ByteArray(bufferInfo.size)
            frameBuffer.get(sps_pps_buf)
        } else if (frameType == iDRType) {//判断是否为I帧
            //I帧不能单独发，需要组装一帧数据：SPS+IDR
            val tempBuf = ByteArray(bufferInfo.size)
            frameBuffer.get(tempBuf)
            val sps_idr_buf = ByteArray(sps_pps_buf.size + tempBuf.size)
            System.arraycopy(sps_pps_buf, 0, sps_idr_buf, 0, sps_pps_buf.size)
            System.arraycopy(tempBuf, 0, sps_idr_buf, sps_pps_buf.size, tempBuf.size)
            socketSend(sps_idr_buf)
        } else {//非I帧的数据直接发就行了。
            val frameBytes = ByteArray(bufferInfo.size)
            frameBuffer.get(frameBytes)
            socketSend(frameBytes)
        }
    }

    private fun socketSend(byteArray: ByteArray) {
        if (this::socketLivePush.isInitialized) {
            socketLivePush.sendData(byteArray)
            Log.e(TAG, "sendFrame: ${byteArray.contentToString()}")
        } else throw Exception("socketLive is not initialized!!")
    }


}