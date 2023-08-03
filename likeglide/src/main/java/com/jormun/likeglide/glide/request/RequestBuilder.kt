package com.jormun.likeglide.glide.request

import android.util.Log
import android.widget.ImageView
import com.jormun.likeglide.glide.GlideContext
import com.jormun.likeglide.glide.RequestManager
import com.jormun.likeglide.glide.ViewTarget
import java.io.File

/**
 * 负责接收图片加载的请求参数。
 * 然后封装成一个Request。
 * 封装完后交给RequestManager来对任务进行管理。
 * 这个类本身并不会对任务进行开启关闭等管理操作，这只是一个负责构建Request的类。
 */
class RequestBuilder(
    private var glideContext: GlideContext,
    private var requestManager: RequestManager
) {
    private val TAG = "RequestBuilder"
    private lateinit var model: Any
    private var requestOptions: RequestOptions = glideContext.defaultRequestOptions

    /**
     * 使用者可以根据自己需要传入Option
     */
    fun apply(requestOptions: RequestOptions): RequestBuilder {
        this.requestOptions = requestOptions
        return this
    }

    /**
     * 用String类型参数请求加载图片
     */
    fun load(stringModel: String): RequestBuilder {
        this.model = stringModel
        return this
    }

    /**
     * 用File类型得参数请求加载图片
     */
    fun load(fileModel: File): RequestBuilder {
        this.model = fileModel
        return this
    }

    /**
     * 根据传入的View构建组件，Target，Request。
     * 构建完毕后加入到RequestManager里面，让其管理这个任务。
     */
    fun into(view: ImageView) {
        if (!this::model.isInitialized) {
            Log.e(TAG, "into: please invoke load() first! ")
            throw Exception("model is null, please invoke load() first!")
        }

        //三部曲
        //1、把传入的View作为Target设置好
        val target = ViewTarget(view)

        //2、图片加载任务的执行者
        val request = Request(glideContext, requestOptions, target, model)

        //3、让RequestManager对这个任务进行管理
        requestManager.track(request)
    }
}