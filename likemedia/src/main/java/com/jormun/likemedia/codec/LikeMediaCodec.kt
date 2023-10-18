package com.jormun.likemedia.codec

import android.util.Log
import com.jormun.likemedia.utils.VideoUtils
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.experimental.and

/**
 * 模拟MediaCodec解析H264码流。
 * 只作学习用。
 */
class LikeMediaCodec(private val mediaPath: String) {

    private var startBit = 0//对二进制数据解析的指针位置，从0开始。
    private val TAG = "LikeMediaCodec"


    fun startCodec() {
        try {
            val mediaFileBytes = getMediaFileBytes(mediaPath)
            val totalSize = mediaFileBytes.size
            var startIndex = 0
            while (true) {
                if (totalSize == 0 || startIndex > totalSize) {
                    break
                }
                //分隔符不参与编码，根据分隔符找到每一帧数据
                val nextFrameStartIndex =
                    VideoUtils.findNextFrame(mediaFileBytes, startIndex + 2, totalSize)
                if (nextFrameStartIndex != -1) {
                    //第一次读取，直接跳过前面的00 00 01分隔符。
                    if (startBit == 0) startBit = 4 * 8
                    //抛弃位
                    val forbidden_zero_bit = u(1, mediaFileBytes)
                    //抛弃位有效直接抛弃
                    if (forbidden_zero_bit != 0) continue
                    printLog("forbidden_zero_bit: $forbidden_zero_bit")
                    //优先级，看看即可，忽略
                    val nal_ref_idc = u(2, mediaFileBytes)
                    printLog("nal_ref_idc: $nal_ref_idc")
                    //帧类型，我们大概装个样子意思一下就行，不需要真的实现
                    val nal_unit_type = u(5, mediaFileBytes)
                    printLog("nal_unit_type: $nal_unit_type")
                    switchNalUnitType(nal_unit_type, mediaFileBytes)
                    break
                } else break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 根据帧类型决定编解码方式
     */
    private fun switchNalUnitType(nalUnitType: Int, mediaFileBytes: ByteArray) {
        when (nalUnitType) {
            1 -> {}
            2 -> {}
            3 -> {}
            4 -> {}
            5 -> {}
            6 -> {}
            7 -> {//sps
                parseSps(mediaFileBytes)
            }
        }
    }

    /**
     * 模拟解码sps的过程
     */
    private fun parseSps(mediaFileBytes: ByteArray) {
        //编码等级
        val profile_idc = u(8, mediaFileBytes)
        printLog("profile_idc: $profile_idc")
        val constraint_set0_flag = u(1, mediaFileBytes)
        val constraint_set1_flag = u(1, mediaFileBytes)
        val constraint_set2_flag = u(1, mediaFileBytes)
        val constraint_set3_flag = u(1, mediaFileBytes)
        val reserved_zero_4bits = u(4, mediaFileBytes)
        val level_idc = u(8, mediaFileBytes)
        printLog("level_idc: $level_idc")
        //sps id
        val seq_parameter_set_id = ue(mediaFileBytes)

        if (profile_idc == 100 || profile_idc == 110 || profile_idc == 122 || profile_idc == 144) {
            //YUV420、422、444
            val chroma_format_idc = ue(mediaFileBytes)
            printLog("chroma_format_idc: $chroma_format_idc")
            if (chroma_format_idc == 3) {
                val residual_colour_transform_flag = u(1, mediaFileBytes)
            }
            val bit_depth_luma_minus8 = ue(mediaFileBytes)
            val bit_depth_chroma_minus8 = ue(mediaFileBytes)
            val qpprime_y_zero_transform_bypass_flag = u(1, mediaFileBytes)
            val seq_scaling_matrix_present_flag = u(1, mediaFileBytes)
        }
        val log2_max_frame_num_minus4 = ue(mediaFileBytes)
        val pic_order_cnt_type = ue(mediaFileBytes)
        if (pic_order_cnt_type == 0) {
            val log2_max_pic_order_cnt_lsb_minus4 = ue(mediaFileBytes)
        }
        val num_ref_frames = ue(mediaFileBytes)
        val gaps_in_frame_num_value_allowed_flag = u(1, mediaFileBytes)

        //***** 关键，宽高，16的倍数。*****
        val pic_width_in_mbs_minus1 = ue(mediaFileBytes)
        //16*倍数才是真正的总宽
        val totalWidth = (pic_width_in_mbs_minus1 + 1) * 16//+1是为了防0
        printLog("totalWidth: $totalWidth")
        val pic_height_in_map_units_minus1 = ue(mediaFileBytes)
        //16*倍数才是真正的总高
        val totalHeight = (pic_height_in_map_units_minus1 + 1) * 16//+1是为了防0
        printLog("totalHeight: $totalHeight")

        val frame_mbs_only_flag = u(1, mediaFileBytes)
        if (frame_mbs_only_flag != 0) {
            val mb_adaptive_frame_field_flag = u(1, mediaFileBytes)
        }
        val direct_8x8_inference_flag = u(1, mediaFileBytes)

        //***** 关键，这个值是假设原视频宽高不是16的倍数时的余数，也就是偏移量。*****
        //视频最终的总宽高是 宽/高+偏移量
        //比如原视频为 1832宽，除以16余0.5，0.5*16=8，也就是最后会偏移8个像素大小，最终变成1840宽
        //可以类比的理解为，View的总宽高是内部 view宽高+margin/padding
        val frame_cropping_flag = u(1, mediaFileBytes)
        if (frame_cropping_flag != 0) {
            //下面分别为具体的偏移量，有左上右下四个方向：
            val frame_crop_left_offset = ue(mediaFileBytes)
            val frame_crop_right_offset = ue(mediaFileBytes)
            val frame_crop_top_offset = ue(mediaFileBytes)
            val frame_crop_bottom_offset = ue(mediaFileBytes)

            //那么视频的真正宽高就是需要减去这些偏移量了：
            val realWidth =
                totalWidth - (frame_crop_left_offset * 2) - (frame_crop_right_offset * 2)
            printLog("realWidth: $realWidth")
            val realHeight =
                ((2 - frame_mbs_only_flag) * totalHeight) - (frame_crop_top_offset * 2) - (frame_crop_bottom_offset * 2)
            printLog("realHeight: $realHeight")
            //为什么是offset*2？因为余数总是2的倍数，因为264的宏块编码只有4、8、16三种形式。
            //height前面要乘(2 - frame_mbs_only_flag)是固定算法，记住就行。
        }

    }

    /**
     * 从文件中读取一部分数据出来，转成byte字节数组。
     */
    @Throws(IOException::class)
    fun getMediaFileBytes(path: String): ByteArray {
        val mIs = DataInputStream(FileInputStream(File(path)))
        var len = 0
        val size = 1024 * 1024
        var buf = ByteArray(size)
        val bos = ByteArrayOutputStream()
        len = mIs.read(buf, 0, size)
        bos.write(buf, 0, len)
        buf = bos.toByteArray()
        bos.close()
        return buf
    }

    /**
     * 模仿H264解析符的u(n)
     * 定长编码
     * @param bitNum: 需要读取多少位
     * @param binData: 被读取的二进制数组
     */
    private fun u(bitNum: Int, binData: ByteArray): Int {
        var result = 0
        //根据传入的位数循环读取到指定位置为止
        for (i in 0 until bitNum) {
            //每次读取指针都右移，那么返回值就要左移，先往低位填上0，等于乘2
            result = result shl 1
            //取出该字节的数据，为当前指针位置/8，因为每次都是按8位1字节读取，只有当前字节已经全部读取完了才会到下个字节。
            val indexData = binData[startBit / 8].toInt()
            //因为计算机一次只能读取一字节数据，所以我们要取多少位实际上就是让蒙版数右移多少次去取有效位。
            // startBit%8 就确保了每次右移在0-7内，并且随着startBit+1而+1。
            //假设第一次 startBit = 32，读取一位，那么%8就是0，此时读取完毕startBit++退出函数。
            //第二次进来，startBit = 33，同样读取一位，那么%8就是1，恰好续上上次读取的位置继续往下读该字节数据。
            if (indexData and (0x80 shr (startBit % 8)) != 0) {
                //有效则把刚才填的0变成1
                result++
            }
            //移动指针
            startBit++
        }
        return result
    }

    /**
     * 哥伦布编码
     * 模仿h264编码的ue(v)
     */
    private fun ue(binData: ByteArray): Int {
        var zeroNum = 0

        //找到0的个数
        while (startBit < binData.size * 8) {
            val indexData = binData[startBit / 8].toInt()
            if (indexData and (0x80 shr startBit % 8) != 0) {
                startBit++
                break
            }
            zeroNum++
            startBit++
        }


        //把1左移回去还原最高位。
        val headNum = (1 shl zeroNum)
        //后续部分有效位个数(1)的和。
        var vCountNum = 0
        //根据0的个数按序读取有效位
        for (i in 0 until zeroNum) {
            vCountNum = vCountNum shl 1
            val indexData = binData[startBit / 8].toInt()
            if (indexData and (0x80 shr startBit % 8) != 0) {
                vCountNum += 1
            }
            startBit++
        }
        //把头尾两数相加即可，哥伦布还要-1。
        val result = (headNum + vCountNum) - 1
        return result
    }

    private fun printLog(msg: String) {
        Log.e(TAG, msg)
    }
}