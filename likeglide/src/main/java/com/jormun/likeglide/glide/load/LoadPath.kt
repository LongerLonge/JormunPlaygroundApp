package com.jormun.likeglide.glide.load

import android.graphics.Bitmap
import android.util.Log
import com.jormun.likeglide.glide.load.codec.ResourceDecoder

/**
 * 防止有多个解码器符合条件。
 * 这个类接收并且封装多个解码器，同时遍历它们来过滤出真正要执行的解码器。
 *@param decoders 符合条件并且需要被封装的多个解码器列表(也可能里面只有一个)
 */
class LoadPath<Data>(
    private val dataClass: Class<Data>,
    //
    private val decoders: List<ResourceDecoder<Data>>
) {
    private val TAG = "LoadPath"

    /**
     * 假如有多个解码器，就遍历并且过滤出真正的解码器。
     * 同时也是通过调用decoder.decode 真正执行解码方法的类。
     * @param data 数据类型，比如InputStream
     * @param width 宽
     * @param height 高
     * @return 统一解码成Bitmap返回
     */
    fun runLoad(data: Data, width: Int, height: Int): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            //遍历decoders列表，筛选出唯一符合的decoder来执行解码工作
            for (decoder in decoders) {
                if (decoder.handles(data)) {//成功则说明符合，筛选成功
                    //交给decoder去解码
                    bitmap = decoder.decode(data, width, height)
                }
                if (bitmap != null) break
            }
        } catch (e: Exception) {
            Log.e(TAG, "runLoad: $data load fail, msg: ${e.message}")
            e.printStackTrace()
        }
        return bitmap

    }
}