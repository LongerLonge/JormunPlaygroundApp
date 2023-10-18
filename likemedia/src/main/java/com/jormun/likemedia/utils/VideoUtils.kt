package com.jormun.likemedia.utils

import android.media.MediaCodec
import com.jormun.likemedia.codec.CodeType
import java.nio.ByteBuffer

object VideoUtils {
    private val TAG = "H264Encoder"

    private val NAL_VPS = 32
    private val NAL_265_SPS = 33
    private val NAL_265_IDR = 19
    private val NAL_264_SPS = 7
    private val NAL_264_IDR = 5

    private lateinit var sps_pps_buf: ByteArray

    /**
     * 解析码流数据
     * @param frameBuffer: 从dsp容器中取出来的buffer数据。
     * @param bufferInfo: MediaCodec.BufferInfo
     * @param codeType: CodeType，用来区分264、265
     */
    fun dealFrame(
        frameBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, codeType: CodeType
    ): ByteArray? {
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
            return sps_idr_buf
        } else {//非I帧的数据直接发就行了。
            val frameBytes = ByteArray(bufferInfo.size)
            frameBuffer.get(frameBytes)
            return frameBytes
        }
        return null
    }

    fun dealSpsPpsFrame(
        frameBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, codeType: CodeType
    ): ByteArray {
        val frameType = getFrameType(frameBuffer, codeType = codeType)
        val startType = getStartType(codeType)
        if (frameType == startType) {//判断是否为SPS或者VPS
            //因为编码器只会输出一次这部分数据，需要保存起来。
            val temp_sps_pps_buf = ByteArray(bufferInfo.size)
            frameBuffer.get(temp_sps_pps_buf)
            return temp_sps_pps_buf
        }
        return ByteArray(0)
    }

    fun dealFrameWithSps(
        spsBytes: ByteArray,
        frameBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, codeType: CodeType
    ): ByteArray {
        if (spsBytes.isEmpty()) throw Exception("sps_pps info is empty! ")
        val frameType = getFrameType(frameBuffer, codeType = codeType)
        val idrType = getIDRType(codeType = codeType)
        sps_pps_buf = spsBytes
        if (frameType == idrType) {//判断是否为I帧
            //I帧不能单独发，需要组装一帧数据：SPS+IDR
            val tempBuf = ByteArray(bufferInfo.size)
            frameBuffer.get(tempBuf)
            val sps_idr_buf = ByteArray(sps_pps_buf.size + tempBuf.size)
            System.arraycopy(sps_pps_buf, 0, sps_idr_buf, 0, sps_pps_buf.size)
            System.arraycopy(tempBuf, 0, sps_idr_buf, sps_pps_buf.size, tempBuf.size)
            return sps_idr_buf
        } else {//非I帧的数据直接发就行了。
            val frameBytes = ByteArray(bufferInfo.size)
            frameBuffer.get(frameBytes)
            return frameBytes
        }
    }

    private fun getFrameType(frameBuffer: ByteBuffer, codeType: CodeType): Int {
        var mask = 0x1F
        if (codeType == CodeType.H265) {
            mask = 0x7E
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
        return frameType
    }

    private fun getStartType(codeType: CodeType): Int {
        var startType = NAL_264_SPS
        if (codeType == CodeType.H265) {
            startType = NAL_VPS
        }
        return startType
    }

    private fun getIDRType(codeType: CodeType): Int {
        var iDRType = NAL_264_IDR
        if (codeType == CodeType.H265) {
            iDRType = NAL_265_IDR
        }
        return iDRType
    }

    //暂时理解到这里
    /**
     * 把NV12的数组旋转90°输出到新的容器数组。
     * @param data: NV12的数组
     * @param output: 需要保存进去的新容器数组
     * @param width: 宽
     * @param height: 高
     */
    fun portraitData2Raw(data: ByteArray, output: ByteArray, width: Int, height: Int) {
        //把这个数组想象成一个无数像素的网格图，上面能看到并且显示的部分是Y数据，下面看不见的隐形部分是UV数据。

        //求出YUV中Y的长度，很好算，Y是每个像素都有，所以就是宽*高
        val y_len = width * height
        // uv数据高为高度的一半，右移一位等于除以2
        val uvHeight = height shr 1
        var k = 0
        //把这个数组想象成一个无数像素的网格图，然后从左到右一列列遍历，每一列从下到上遍历。
        for (j in 0 until width) {//外循环是移动列，一次移动一列
            for (i in height - 1 downTo 0) {//内循环是遍历列内数据，从下往上
                //旋转Y，并且存到新数组里
                output[k++] = data[width * i + j]
            }
        }
        //把UV数据存到新容器里面，起点位置为Y数据的末尾。
        var j = 0
        //跟Y不一样的是，uv因为不需要参与旋转，所以直接把原数组里面的uv数据按顺序存到新容器中即可。
        while (j < width) {//外循环是移动列(UV为每次移动2列)
            for (i in uvHeight - 1 downTo 0) {//内循环是遍历列内数据，从最下面，左往右遍历。
                output[k++] = data[y_len + width * i + j]
                output[k++] = data[y_len + width * i + j + 1]
            }
            j += 2
        }
    }

    /**
     * 把NV21数组转换成NV12数组
     * 实际上就是把里面uv数据的顺序整理成我们需要的样子
     * yyyyvuvu -> yyyyuvuv
     */
    fun nv21toNV12(nv21: ByteArray): ByteArray {
//        nv21   0----nv21.size
        val size = nv21.size
        val nv12 = ByteArray(size)
        //求出Y在原来的总数组中的长度，y=4*(size/6)=(2/3)*size
        val len = size * 2 / 3
        //把Y都复制进去新数组里
        System.arraycopy(nv21, 0, nv12, 0, len)
        //把原来的总数组中剩下的UV数据按照我们需要的顺序存放到新数组(uuuvvv)
        var i = len//起点是Y的末尾
        while (i < size - 1) {
            //因为21是 V、U、V、U这样排列，所以我们倒过来UV这样存
            nv12[i] = nv21[i + 1]
            nv12[i + 1] = nv21[i]
            i += 2
        }
        return nv12
    }

    /**
     * 需要解析一下，看是音频还是视频数据
     */
    fun encodeDataType(data: ByteArray): Map<Int, ByteArray> {
        val result = mutableMapOf<Int, ByteArray>()
        val srcBuff = ByteArray(data.size - 1)
        System.arraycopy(data, 1, srcBuff, 0, data.size - 1)
        result[data[0].toInt()] = srcBuff
        return result
    }


    /**
     * 从H264字节数据中，找到下一帧的起始位置，同时也是当前帧的结束位置
     */
    fun findNextFrame(byteArray: ByteArray, start: Int, total: Int): Int {
        for (i in start..total - 4) {
            //找的方法很简单，就是读取4个字节的数据，是否满足 00 00 00 01这个分隔符即可
            val b1 = byteArray[i].toInt()
            val b2 = byteArray[i + 1].toInt()
            val b3 = byteArray[i + 2].toInt()
            val b4 = byteArray[i + 3].toInt()
            if ((b1 == 0x00 && b2 == 0x00 && b3 == 0x00 && b4 == 0x01)) {
                //如果满足，说明当前下标就是分隔符，返回即可
                return i
            }
        }
        return -1
    }
}