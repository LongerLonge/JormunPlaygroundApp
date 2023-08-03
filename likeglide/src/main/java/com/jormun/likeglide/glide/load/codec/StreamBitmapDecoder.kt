package com.jormun.likeglide.glide.load.codec

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.jormun.likeglide.glide.cache.ArrayPool
import com.jormun.likeglide.glide.recycle.BitmapPool
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 解码器
 * @param bitmapPool 顾名思义，Bitmap缓存池，一般来说GlideContext有。
 * @param arrayPool 数组缓存池，用来给MarkInputStream用的，优化数组加载的方式。
 * 包含一个关键类
 * @see MarkInputStream
 */
class StreamBitmapDecoder(private val bitmapPool: BitmapPool, private val arrayPool: ArrayPool) :
    ResourceDecoder<InputStream> {
    private val TAG = "StreamBitmapDecoder"

    override fun handles(source: InputStream): Boolean {
        return true
    }

    override fun decode(source: InputStream, width: Int, height: Int): Bitmap? {
        Log.e(TAG, "decode: 解码图片开始！")
        //先把source初始化成我们自己实现的MarkInputStream
        val markInputStream: MarkInputStream = if (source is MarkInputStream) {
            source
        } else {
            MarkInputStream(source, arrayPool)
        }
        //MarkInputStream标记回溯点(这里是直接把头作为回溯点，也就是0)
        markInputStream.mark(0)

        //通过Options来只读取宽高等描述信息
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        //注意这里调用了decodeStream，同时传入了我们自己实现的MarkInputStream，实际上这里就发生read行为了，也就是
        //MarkInputStream里面有一部分数据已经被读取，且MarkInputStream就负责记录这部分被读取的数据，同时缓存进一个Buffer中。
        //因为这里只是读取宽高，这部分数据位于InputStream的前面一小段，我们缓存进去buffer中就可以避免后面再读的时候又要从头开始
        //且byte[]数组需要多次创建的内存抖动问题(可以直接从数组缓存池中拿)。
        BitmapFactory.decodeStream(markInputStream, null, options)
        options.inJustDecodeBounds = false//取消只读描述
        //从描述信息中取出数据源的宽高
        val sourceWidth = options.outWidth
        val sourceHeight = options.outHeight
        //判断是否有传入目标宽高，有就用传入的目标宽高没有就用源数据的
        val targetWidth = if (width < 0) sourceWidth else width
        val targetHeight = if (height < 0) sourceHeight else height
        //算出目标宽高和源数据宽高的比值
        val widthFactor = targetWidth / (sourceWidth.toFloat())
        val heightFactor = targetHeight / (sourceHeight.toFloat())
        //取宽高比值中最大的比值
        val maxFactor = max(heightFactor, widthFactor)
        //把源图片的宽高都乘以最大比值，得出缩放后的宽高
        val outWidth = (maxFactor * sourceWidth).roundToInt()
        val outHeight = (maxFactor * sourceHeight).roundToInt()

        //分别获得宽、高需要缩放多大
        // 宁愿小一点 不超过需要的宽、高
        //算出缩放后的宽高和源图片宽高是否能整除，可以整除就代表源图片宽高和缩放后的宽高是整数倍
        //是整数倍那么直接除以得出缩放倍数(因子)，整除不了的话就除以后+1，让缩放因子更大一些。注意这个因子越大缩小得越厉害
        val widthScaleFactor =
            if (sourceWidth % outWidth == 0) sourceWidth / outWidth else sourceWidth / outWidth + 1
        val heightScaleFactor =
            if (sourceHeight % outHeight == 0) sourceHeight / outHeight else sourceHeight / outHeight + 1
        //还是老规矩，取最大那个，注意这个因子越大缩小得越厉害
        var sampleSize = max(widthScaleFactor, heightScaleFactor)
        //看下输缩放倍数大还是1大，取最大值
        sampleSize = max(1, sampleSize)
        //设置缩放倍数,注意这个倍数越大缩小得越厉害
        options.inSampleSize = sampleSize//1代表原图无缩放，Android官方建议是2的整数倍
        //RGB565 更省空间
        options.inPreferredConfig = Bitmap.Config.RGB_565
        //从BitmapPool中拿可以复用的Bitmap
        val bm = bitmapPool.get(outWidth, outHeight, Bitmap.Config.RGB_565)
        //设置bitmap
        options.inBitmap = bm
        //可复用，设置这个才能让Bitmap进行复用
        options.inMutable = true
        //重置markInputStream的回溯点(这里是直接返回到头也就是0)
        markInputStream.reset()
        //然后传入markInputStream进行Bitmap解析，因为我们已经缓存过已读取的部分，所以已读取的部分直接可以从缓存中拿
        val finalBitmap = BitmapFactory.decodeStream(markInputStream, null, options)
        //释放流
        markInputStream.release()
        Log.e(TAG, "decode: 解码图片完成！")
        return finalBitmap
    }
}