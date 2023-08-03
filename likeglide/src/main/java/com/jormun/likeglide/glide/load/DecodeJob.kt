package com.jormun.likeglide.glide.load

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.NonNull
import com.jormun.likeglide.glide.GlideContext
import com.jormun.likeglide.glide.bean.Key
import com.jormun.likeglide.glide.bean.Resources
import com.jormun.likeglide.glide.cache.take.DiskCache
import com.jormun.likeglide.glide.load.generator.DataCacheGenerator
import com.jormun.likeglide.glide.load.generator.DataGenerator
import com.jormun.likeglide.glide.load.generator.SourceGenerator
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * 协调进行磁盘数据加载和网络数据加载的类。
 * 还是通过组合的方式来实现这些功能。
 * 本身就是一个Runnable，也就是子线程执行对象，该类里面所有行为都是在子线程执行的。
 * 这个类值得学习的是如何处理状态切换和执行的设计，见runGenerators()
 *
 * @param glideContext 上下文，这里是为了获取Registry来获得解码器
 * @param diskCache 磁盘缓存，用来往里面存取数据用的
 * @param model 传入的数据类，比如一串String类型的url
 * @param decodeJobCallback 监听接口，在这里DecodeJob是被EngineJob监听
 *
 */
class DecodeJob(
    private var glideContext: GlideContext,
    private var diskCache: DiskCache,
    private var model: Any,
    private var width: Int,
    private var height: Int,
    private var decodeJobCallback: DecodeJobCallback
) : Runnable, DataGenerator.DataGeneratorCallback {

    private val TAG = "DecodeJob"

    private var currentGenerator: DataGenerator? = null
    private var isCancelled = false
    private var isCallbackNotified = false
    private var stage: Stage? = null
    private var sourceKey: Key? = null

    interface DecodeJobCallback {
        fun onResourceReady(resources: Resources)
        fun onLoadFailed(e: Exception)
    }

    private enum class Stage {
        INITIALIZE, //初始化阶段
        DATA_CACHE, //查找文件缓存阶段
        SOURCE, FINISHED,
    }

    fun cancel() {
        isCancelled = true
        currentGenerator?.cancel()
    }

    /**
     * 通过子线程的方式去加载图片
     */
    override fun run() {
        try {
            Log.e(TAG, "开始加载数据")
            if (isCancelled) {
                Log.e(TAG, "取消加载数据")
                decodeJobCallback.onLoadFailed(Exception("Load Canceled"))
                return
            }
            stage = getNextStage(Stage.INITIALIZE)
            currentGenerator = getNextGenerator()
            runGenerators()
        } catch (e: Exception) {
            decodeJobCallback.onLoadFailed(e)
        }
    }

    /**
     * 获取对应的数据生成器。
     * 顺序如下：
     * stage->cache：先去磁盘缓存找。
     * stage->source：去网络或者文件加载。
     */
    private fun getNextGenerator(): DataGenerator? {
        when (stage) {
            Stage.DATA_CACHE -> {
                Log.e(TAG, "使用磁盘缓存加载器")
                return DataCacheGenerator(glideContext, diskCache, model, this)
            }

            Stage.SOURCE -> {
                //负责从图片源地址加载数据的生成器
                Log.e(TAG, "使用源资源加载器")
                return SourceGenerator(glideContext, model, this)
            }

            Stage.FINISHED -> {
                return null
            }

            else -> {
                throw Exception("Unrecognized stage: $stage")
            }
        }
    }

    private fun runGenerators() {
        // 使用了对应的生成器开始加载了
        var isStarted = false
        //while循环来不断推进并执行Generator
        while (!isCancelled && currentGenerator != null && !isStarted) {
            //目前有两个生成器，一个是磁盘缓存生成器，第二个是网络加载生成器。
            isStarted = currentGenerator!!.startNext()
            // 生成器工作了 就break
            //如果磁盘生成找到了，说明有缓存就break。
            //否则就是加载网络数据，让网络加载生成器去工作。
            if (isStarted) {
                break
            }
            //获得下一个阶段
            stage = getNextStage(stage!!)
            if (stage == Stage.FINISHED) {
                Log.e(TAG, "状态结束,没有加载器能够加载对应数据")
                break
            }
            //通过当前的阶段获得 下一个生成器，返回while判断currentGenerator是否为空，不为空就继续循环
            currentGenerator = getNextGenerator()
        }
        if ((stage == Stage.FINISHED || isCancelled) && !isStarted) {
            notifyFailed()
        }
    }

    private fun notifyFailed() {
        Log.e(TAG, "加载失败")
        if (!isCallbackNotified) {
            isCallbackNotified = true
            decodeJobCallback.onLoadFailed(Exception("Failed to load resource"))
        }
    }

    private fun getNextStage(current: Stage): Stage {
        when (current) {
            //第一步状态，初始化，那就往下推进到缓存查找
            Stage.INITIALIZE -> {
                return Stage.DATA_CACHE
            }
            //缓存查找没有，就把状态再往下推进到源数据加载(网络)
            Stage.DATA_CACHE -> {
                return Stage.SOURCE
            }
            //源数据加载和完成，那就完成
            Stage.SOURCE, Stage.FINISHED -> {
                return Stage.FINISHED
            }

            else -> {
                throw Exception("Unrecognized stage: $current")
            }
        }
    }

    override fun onDataReady(
        key: Key?, data: Any, dataSource: DataGenerator.DataGeneratorCallback.DataSource
    ) {
        this.sourceKey = key
        Log.e(TAG, "加载成功,开始解码数据")
        //数据加载过来了，要执行解码
        // InputStream ->>> Bitmap
        runLoadPath<Any>(data, dataSource)
    }

    override fun onDataFetcherFailed(key: Key?, e: Exception) {
        //再次运行 失败的话 状态变为finish则 结束
        Log.e(TAG, "加载失败，尝试使用下一个加载器:" + e.message)

    }

    /**
     * 解码
     * @param data 需要解码的数据，比如inputStream，
     * @param dataSource 数据类型，比如缓存或者网络数据等
     * @param <Data>
     */
    private fun <Data> runLoadPath(
        data: Data,
        dataSource: DataGenerator.DataGeneratorCallback.DataSource
    ) {
        //LoadPath实际上就是包装了Decoder的类而已
        val loadPath: LoadPath<Data> =
            glideContext.registry.getLoadPath(data!!::class.java as Class<Data>)
        //runLoad实际上就是调用里面包装好的Decoder解码而已
        val bitmap = loadPath.runLoad(data, width, height)
        if (bitmap != null) {
            Log.e(TAG, "解码成功回调")
            notifyComplete(bitmap, dataSource)
        } else {
            Log.e(TAG, "解码失败，尝试使用下一个加载器")
            runGenerators()
        }
    }

    /**
     * 成功，写入磁盘缓存并且回调
     * @param bitmap
     * @param dataSource
     */
    private fun notifyComplete(
        bitmap: Bitmap,
        dataSource: DataGenerator.DataGeneratorCallback.DataSource
    ) {
        //如果任务已关闭，不需要写入磁盘缓存
        //为了保证单次加载的完整性，不然大图写入一部分后就中断的话是浪费空间。
        if (isCancelled) {
            decodeJobCallback.onLoadFailed(Exception("jobs canceled."))
            return
        }
        //判断是否来源于原始数据，比如网络或者本地未缓存的文件，如果是就加入到磁盘缓存
        if (dataSource === DataGenerator.DataGeneratorCallback.DataSource.REMOTE) {
            //写入文件缓存
            diskCache.put(sourceKey, object : DiskCache.Writer {
                override fun write(file: File): Boolean {
                    var os: FileOutputStream? = null
                    try {
                        os = FileOutputStream(file)
                        //压缩一下
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
                        return true
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } finally {
                        if (null != os) {
                            try {
                                os.close()
                            } catch (e: IOException) {
                                Log.e(TAG, "write err: ${e.message}")
                            }
                        }
                    }
                    return false
                }
            })
        }
        //写完文件缓存后，把Bitmap封装成Resources并且回调给上面。
        val resource = Resources(bitmap)
        decodeJobCallback.onResourceReady(resource)
    }
}