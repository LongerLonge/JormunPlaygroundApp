package com.jormun.likemedia.codec

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.util.Log
import com.jormun.likemedia.cons.MediaCodeType
import com.jormun.likemedia.cons.VideoEncodeFormat
import com.jormun.likemedia.net.SocketLivePush
import com.jormun.likemedia.utils.FileUtils
import com.jormun.likemedia.utils.VideoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 负责把录制的数据编码成H264格式的类。
 * @param mediaProjection: Android默认编码器，这个类无法字节创建，需要外界提供。
 * @param isStream: 是否需要推送到网络上的视频流。
 * @param codeType: 编码类型。
 */
class H264Encoder(
    private val mediaProjection: MediaProjection,
    private val width: Int = VideoEncodeFormat.VIDEO_WIDTH,
    private val height: Int = VideoEncodeFormat.VIDEO_HEIGHT,
    private val isStream: Boolean = false,
    private val codeType: CodeType
) : BaseEncoder {

    private val TAG = "H264Encoder"

    private lateinit var mediaCodec: MediaCodec

    private lateinit var virtualDisplay: VirtualDisplay

    private lateinit var socketLivePush: SocketLivePush


    init {
        initEncoderData()
    }

    fun setTheSocketLive(socketLivePush: SocketLivePush) {
        this.socketLivePush = socketLivePush
    }

    private fun initEncoderData() {
        //创建编码格式类
        val encoderFormat = MediaFormat.createVideoFormat(VideoEncodeFormat.VIDEO_MIMETYPE, width, height)

        try {
            //创建编码类型的MediaCodec
            mediaCodec = MediaCodec.createEncoderByType(VideoEncodeFormat.VIDEO_MIMETYPE)
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
    override fun startEncoder() {
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
                            //判断是否需要在线输出码流数据
                            if (!isStream) {
                                get(byteArray)
                                //把字节码写到文件
                                FileUtils.writeBytes(byteArray, "codec.h264")
                                //把字节码以十六进制的格式写入(h265只能解码十六进制)
                                FileUtils.writeContent(byteArray, "codecH264")
                            } else {
                                //把码流数据解析成Byte数组
                                val frameBytes = VideoUtils.dealFrame(this, bufferInfo, codeType)
                                frameBytes?.apply {
                                    socketSend(frameBytes)
                                }
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

    override fun dealSpsFrameAndSave(frameBuffer: ByteArray) {

    }

    /**
     * 利用Socket发送码流数据。
     * @param byteArray: 已经解析好的码流数据。
     */
    private fun socketSend(byteArray: ByteArray) {
        if (this::socketLivePush.isInitialized) {
            socketLivePush.sendData(byteArray,MediaCodeType.VIDEO_DATA)
            Log.e(TAG, "sendFrame: ${byteArray.contentToString()}")
        } else throw Exception("socketLive is not initialized!!")
    }


}