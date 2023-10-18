package com.jormun.likemedia.codec

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.jormun.likemedia.utils.FileUtils
import com.jormun.likemedia.utils.PcmToWavUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer


object MusicProcess {
    const val TAG = "MusicProcess"
    const val TIME_OUT = 1000L

    /**
     * 把视频和音频进行混音
     * @param videoPath: 视频文件地址
     * @param audioPath: 音频文件地址
     * @param outputPath: 输出的文件路径
     * @param cacheDirPath: 混音过程会产生缓存文件，需要提供缓存文件存放地址
     * @param startTime: 视频开始时间
     * @param endTime: 视频结束时间
     * @param videoVolume: 视频混音的音量大小
     * @param musicVolume: 音频混音的音量大小
     */
    suspend fun mixAudioTrack(
        videoPath: String,
        audioPath: String,
        outputPath: String,
        cacheDirPath: String,
        startTime: Int,
        endTime: Int,
        videoVolume: Float,
        musicVolume: Float
    ) {
        try {
            //在cache目录下生成缓存的pcm文件
            val cacheVideoPcm = File(cacheDirPath, "video.pcm")
            val cacheAudioPcm = File(cacheDirPath, "audio.pcm")
            val mixPcmFile = File(cacheDirPath, "mixPcm.pcm")
            //Log.e(TAG, "mixAudioTrack: path(${cacheVideoPcm.absolutePath})")

            //解码视频，提取pcm
            decodeToPcm(videoPath, cacheVideoPcm.absolutePath, startTime, endTime)
            //解码音频，提取pcm
            decodeToPcm(audioPath, cacheAudioPcm.absolutePath, startTime, endTime)

            //把两者pcm混到一起
            mixPCM(
                cacheVideoPcm.absolutePath,
                cacheAudioPcm.absolutePath,
                mixPcmFile.absolutePath,
                videoVolume,
                musicVolume
            )

            //先把混合后的pcm转成wav
            val cacheWav = File(cacheDirPath, "cacheMix.wav")
            PcmToWavUtil(
                44100, AudioFormat.CHANNEL_IN_STEREO, 2, AudioFormat.ENCODING_PCM_16BIT
            ).pcmToWav(mixPcmFile.absolutePath, cacheWav.absolutePath)

            //混合PCM后，再把这个PCM跟原视频混合(先转成wav)：
            mixVideoAndPCM(videoPath, cacheWav.absolutePath, outputPath, startTime, endTime)


        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "mixAudioTrack: err.")
        }
    }

    private fun mixVideoAndPCM(
        videoPath: String, audioFilePath: String, outputPath: String, startTime: Int, endTime: Int
    ) {
        //视频容器，要注意容器(封装格式)和编码格式的区别：
        val mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)


        //第一步、提取视频里面的画轨并添加到新容器中(给新容器添加一条画轨)
        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(videoPath)
        val videoTrack = selectTrack(videoExtractor, false)
        val videoFormat = videoExtractor.getTrackFormat(videoTrack)
        mediaMuxer.addTrack(videoFormat)

        //第二部、提取视频里面的音轨并添加到新容器中(给新容器添加一条音轨)
        val audioTrack = selectTrack(videoExtractor, true)
        val audioFormat = videoExtractor.getTrackFormat(audioTrack)
        //获取音轨的比特率，后面要用
        val bitRate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE)
        //我们传入的是pcm或者wav，需要强制指定AAC模式
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        //添加了音轨之后会返回音轨的index，这个后面要用请注意
        val muxerAudioIndex = mediaMuxer.addTrack(audioFormat)

        mediaMuxer.start()//开启

        //------------------------------ 下面这里是把音频文件输入到DSP进行编码，并且塞入到容器的音轨中去 ------------------------------
        //第三步读取音频文件的信息，比如最大input size，和它的音轨等。
        //这个Extractor很重要，后面需要反复用到，请注意
        val wavExtractor = MediaExtractor()
        wavExtractor.setDataSource(audioFilePath)
        val selectTrack = selectTrack(wavExtractor, true)//获取音频文件的音轨
        wavExtractor.selectTrack(selectTrack)
        val wavTrackFormat = wavExtractor.getTrackFormat(selectTrack)

        var maxBufferSize = 100 * 1000
        if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) maxBufferSize =
            wavTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)



        //第四步，设置编码格式，采用压缩和音频AAC的方式编码，设置单次处理最大的input size，比特率等
        //注意这里要跟上面音频文件一致，也就是音频文件的Extractor提取出来的信息可以大部分设置到这里
        val encodeAudioFormat =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2)
        //音质等级
        encodeAudioFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        encodeAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)

        encodeAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize)

        //第五步，创建编码器，AAC模式，对音频文件输入数据进行编码
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(encodeAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        //根据单次最大数据input size(音频文件的)，创建缓冲区。
        var buffer = ByteBuffer.allocateDirect(maxBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()//BufferInfo老朋友了

        var encodeDone = false

        while (!encodeDone) {
            //外循环是塞入数据给dsp进行解码
            //还是老规矩，查询dsp输入容器是否有空余，有就返回index。
            val inIndex = encoder.dequeueInputBuffer(10000)
            if (inIndex >= 0) {
                //每循环一次就从Extractor中提取时间戳(pts)
                val sampleTime = wavExtractor.sampleTime
                if (sampleTime < 0) {//代表已经到了末尾
                    //给编码器发通知,让它生成结束标记,不然视频文件不知道哪里是末尾就无法播放
                    encoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    //还没到末尾
                    //从extractor中提取数据到buffer,并返回大小(这里才是数据源)
                    val size = wavExtractor.readSampleData(buffer, 0)
                    val inputBuffer = encoder.getInputBuffer(inIndex)
                    //把提取出来的音频数据buffer塞入dsp输入容器里
                    inputBuffer?.apply {
                        clear()
                        put(buffer)
                        position(0)//重新定位到首位
                        //通知dsp我已经塞好数据了，还有一些解码的参数
                        //这一段最好放里面，放外面首几帧会卡
                        encoder.queueInputBuffer(
                            inIndex,
                            0,
                            size,
                            sampleTime,
                            wavExtractor.sampleFlags//解码的flag要跟音频文件的flag一致
                        )
                        //extractor抛弃这一段数据，准备迎接下一段数据，否则就永远卡在这一段数据
                        wavExtractor.advance()
                    }

                }
            }
            //查询dsp处理完了没
            var outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIME_OUT)
            //这里用while不再用if，因为有可能单次会输出好几个解码完的数据，要全部获取完解码后的数据才重新回到外循环塞入新数据去解码
            while (outIndex >= 0) {
                //判断是否已经完成所有解码了，完成之后直接break内循环就行
                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    encodeDone = true
                    break
                }
                val outputBuffer = encoder.getOutputBuffer(outIndex)
                outputBuffer?.apply {
                    mediaMuxer.writeSampleData(muxerAudioIndex, outputBuffer, bufferInfo)
                    outputBuffer.clear()
                }
                encoder.releaseOutputBuffer(outIndex, false)
                //继续查询这次是否还有数据输出，内循环继续
                outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIME_OUT)
            }
        }

        //------------------------------ 下面这里是把视频的画轨塞入到新容器的画轨中 ------------------------------
        //到这里就代表所有音频数据处理完毕

        //先把视频的音轨取消选中，接下来要处理画轨
        if (audioTrack > 0) {
            videoExtractor.unselectTrack(audioTrack)
        }

        //开始处理画轨，选中画轨
        videoExtractor.selectTrack(videoTrack)
        //跳转到我们需要剪辑的开始位置，前一个I帧
        videoExtractor.seekTo(startTime.toLong(), MediaExtractor.SEEK_TO_NEXT_SYNC)
        //取出单次输入最大数据量，用来创建缓冲区
        maxBufferSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        buffer = ByteBuffer.allocateDirect(maxBufferSize)

        //因为视频文件里面的画轨数据已经处理过了，这里就不需要再去编码再塞入容器，直接塞就行。
        while (true) {
            val sampleTime = videoExtractor.sampleTime
            //seek并不是精确到某一帧，只能是附近的I帧，所以需要判断seek到的帧真是否是我们需要的范围
            if (sampleTime == -1L) {//-1代表已经读到末尾
                break
            } else if (sampleTime < startTime) {//还没到我们指定的开始时间
                videoExtractor.advance()//抛弃，继续获取下一帧
                continue
            } else if (sampleTime > endTime) {//超出结束时间，退出
                break
            }
            //给info设置pts，flags，size
            //bufferInfo.presentationTimeUs = sampleTime - startTime + 600
            bufferInfo.presentationTimeUs = sampleTime - startTime
            bufferInfo.flags = videoExtractor.sampleFlags
            //从extractor中读取数据到buffer,并且返回大小
            bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) {
                break
            }
            //把读取出来的数据塞入到容器中
            mediaMuxer.writeSampleData(videoTrack, buffer, bufferInfo)
            //丢弃这段数据,迎接下一段数据。
            videoExtractor.advance()
        }

        wavExtractor.release()
        videoExtractor.release()
        encoder.stop()
        encoder.release()
        mediaMuxer.release()
    }

    /**
     * 对两个pcm进行混音
     */
    private fun mixPCM(
        pcm1Path: String, pcm2Path: String, outPath: String, vol01: Float, vol02: Float
    ) {

        //打开流
        val pcm1Input = FileInputStream(pcm1Path)
        val pcm2Input = FileInputStream(pcm2Path)
        val fos = FileOutputStream(outPath)
        //每次取2k的数据出来：
        val buff01 = ByteArray(2048)
        val buff02 = ByteArray(2048)
        val buff03 = ByteArray(2048)
        //是否读取完毕的标记位
        var end1 = false
        var end2 = false
        //开始循环读
        while (!end1 || !end2) {
            if (!end2) {
                end2 = (pcm1Input.read(buff01) == -1)
            }
            if (!end1) {
                end1 = (pcm2Input.read(buff02) == -1)
            }
            var mixVoice = 0
            //每次读取2个字节，pcm的保存格式
            for (i in buff02.indices step 2) {

                //byte只能转成int处理
                //int四字节32位，我们要的是1字节8位，所以&(ff)屏蔽掉最后8位前的所有位，等于说只取出32位里面的最后8位。
                //虽然全程是以int计算，但是经过我们上面的mask遮罩后，32位里面只有后8位参与运算，借此来模拟1字节8位的数据运算。
                //因为pcm基于小端存储，前一字节数据不需要动，后一字节数据需要获取8位后往前挪8位，也就是左移8位。
                //两个二进制用或运算，得出组合后的十六位数据
                //比如第一个字节算出来是 (24个0).... 0000 1111，第二个字节是 (16个0).... 0000 1111 0000 0000
                //两者或运算直接就可以组合出我们需要的数据： (16个0).... 0000 1111 0000 1111
                val b1Bit: Short =
                    ((buff01[i].toInt() and 0xff) or ((buff01[i + 1].toInt() and 0xff) shl 8)).toShort()
                val b2Bit: Short =
                    ((buff02[i].toInt() and 0xff) or ((buff02[i + 1].toInt() and 0xff) shl 8)).toShort()

                //从两个pcm中获取的点相加即可得出混音后的点,但是因为有音量大小的存在，所以要先乘以音量来缩放点的大小
                mixVoice = (b1Bit * vol01).toInt() + (b2Bit * vol02).toInt()
                //mixVoice = b1Bit  + b2Bit

                //控制波的上下极值
                if (mixVoice > 32767) mixVoice = 32767
                else if (mixVoice < -32768) mixVoice = 32768

                //把结果按照小端存储方式写入保存：
                buff03[i] = (mixVoice and 0xff).toByte()//只取后8位
                buff03[i + 1] = ((mixVoice shr 8) and 0xff).toByte()//原数据右移8位获得前8位
            }
            //写到目标文件
            fos.write(buff03)
        }
        //好习惯，关闭流
        pcm1Input.close()
        pcm2Input.close()
        fos.close()
        Log.e(TAG, "mixPCM: vol01:$vol01 vol02:$vol02")
    }

    /**
     * 提取PCM
     */
    private fun decodeToPcm(
        inPath: String,
        outputPath: String,
        startTime: Int,
        endTime: Int,
    ) {
        if (endTime < startTime) return
        //利用MediaExtractor提取音频PCM
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(inPath)
        val audioTrack = selectTrack(mediaExtractor, true)
        if (audioTrack == -1) throw Exception("not found audio track!")
        //找到
        mediaExtractor.selectTrack(audioTrack)
        //因为获取的是整段视频的音轨，我们不需要，只需要截取需要部分的音轨：
        mediaExtractor.seekTo(startTime.toLong(), MediaExtractor.SEEK_TO_NEXT_SYNC)//seek到指定时间的下个I帧
        val audioFormat = mediaExtractor.getTrackFormat(audioTrack)
        //创建解码器(解码器可以解码音视频)：
        val typeStr =
            audioFormat.getString(MediaFormat.KEY_MIME) ?: throw Exception("type not found!")
        val mediaCodec = MediaCodec.createDecoderByType(typeStr)
        mediaCodec.configure(audioFormat, null, null, 0)
        mediaCodec.start()
        //获得该音视频文件单次处理的最大字节数量
        var maxBufferSize = 100 * 1000
        if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        }
        //解码器工作，输出PCM文件
        val pcmFile = File(outputPath)
        //这里用了FileChannel，可以理解为高级版的Stream，并且线程安全
        val pcmFileChannel = FileOutputStream(pcmFile).channel
        //Channel需要配合ByteBuffer用
        val channelBuffer = ByteBuffer.allocateDirect(maxBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val inIndex = mediaCodec.dequeueInputBuffer(1000)
            if (inIndex >= 0) {
                //获得这一帧的时间戳(并没开始解码，只是获取某一帧的数据)
                val sampleTime = mediaExtractor.sampleTime
                //seek并不是精确到某一帧，只能是附近的I帧，所以需要判断seek到的帧真是否是我们需要的范围
                if (sampleTime == -1L) {//-1代表已经读到末尾
                    break
                } else if (sampleTime < startTime) {//还没到我们指定的开始时间
                    mediaExtractor.advance()//抛弃，继续获取下一帧
                } else if (sampleTime > endTime) {//超出结束时间，退出
                    break
                }
                //解码前设置一些基本参数
                bufferInfo.size = mediaExtractor.readSampleData(channelBuffer, 0)
                bufferInfo.presentationTimeUs = sampleTime
                bufferInfo.flags = mediaExtractor.sampleFlags
                val contentBytes = ByteArray(channelBuffer.remaining())
                channelBuffer.get(contentBytes)
                FileUtils.writeContent(contentBytes, "video.pcm")

                //设置好之后还是老规矩放入到dsp容器里面让dsp去解码
                val inputBuffer = mediaCodec.getInputBuffer(inIndex)
                inputBuffer?.apply {
                    put(contentBytes)
                    mediaCodec.queueInputBuffer(
                        inIndex, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags
                    )
                }
                //读取了这一帧的数据需要手动释放掉
                mediaExtractor.advance()
            }

            //老规矩，查询dsp容器看看解码好了没，有就拿出来处理
            var outIndex = -1
            outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000)
            Log.e(TAG, "decodeToPcm: outIndex($outIndex)")
            if (outIndex >= 0) {
                val outputBuffer = mediaCodec.getOutputBuffer(outIndex)
                pcmFileChannel.write(outputBuffer)
                mediaCodec.releaseOutputBuffer(outIndex, false)
            }
        }
        pcmFileChannel.close()
        mediaExtractor.release()
        mediaCodec.stop()
        mediaCodec.release()
    }

    /**
     * 利用MediaExtractor提取音轨
     */
    private fun selectTrack(mediaExtractor: MediaExtractor, isAudio: Boolean): Int {
        for (i in 0 until mediaExtractor.trackCount) {
            val trackFormat = mediaExtractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
            mime?.apply {
                if (isAudio) {
                    if (startsWith("audio")) {
                        return i;
                    }
                } else {
                    if (startsWith("video")) {
                        return i;
                    }
                }
            }
        }
        return -1
    }
}