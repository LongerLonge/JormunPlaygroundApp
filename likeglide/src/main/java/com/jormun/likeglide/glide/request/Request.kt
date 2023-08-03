package com.jormun.likeglide.glide.request

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.jormun.likeglide.glide.GlideContext
import com.jormun.likeglide.glide.ViewTarget
import com.jormun.likeglide.glide.bean.Resources
import com.jormun.likeglide.glide.load.Engine

/**
 * 图片加载任务的类。
 * 由RequestManager进行管理。
 *
 */
class Request(
    private var glideContext: GlideContext?,
    private var requestOptions: RequestOptions?,
    private var viewTarget: ViewTarget?,
    private var model: Any?
) : ViewTarget.SizeReadyCallback, ResourceCallback {
    private val TAG = "Request"

    private enum class Status {
        PENDING, RUNNING, WAITING_FOR_SIZE, COMPLETE, FAILED, CANCELLED, CLEARED, PAUSED
    }


    private var status: Status? = null

    private var errDrawable: Drawable? = null

    private var placeholderDrawable: Drawable? = null

    private var resources: Resources? = null

    private var loadStatus: Engine.LoadStatus? = null
    private var engine: Engine? = null

    init {
        engine = glideContext?.engine
    }


    fun recycle() {
        glideContext = null
        model = null
        requestOptions = null
        viewTarget = null
        status = null
        errDrawable = null
        placeholderDrawable = null
    }

    /**
     * 启动数据加载层的工作，让数据加载层开始工作流程。
     */
    fun begin() {
        //设置加载状态为 等待大小的获得
        status = Status.WAITING_FOR_SIZE
        //开始加载 先设置占位图片
        viewTarget?.onLoadStarted(getPlaceholderDrawable())

        //requestOptions 占位图 失败图 固定大小的配置
        requestOptions?.apply {
            //看看requestOptions里面有没有指定固定的宽高，有就直接用
            if (overrideWidth > 0 && overrideHeight > 0) {
                onSizeReady(overrideWidth, overrideHeight)
            } else {
                //没有指定宽高，那就计算View的size 计算完成后会回调 onSizeReady
                viewTarget?.getSize(this@Request)
            }
        } ?: { Log.e(TAG, "begin: requestOptions is null.") }
    }

    /**
     * 是否正在运行
     */
    fun isRunning(): Boolean {
        return status == Status.RUNNING || status == Status.WAITING_FOR_SIZE
    }

    fun isCompleted(): Boolean {
        return status == Status.COMPLETE
    }

    fun isCancelled(): Boolean {
        return status == Status.CANCELLED || status == Status.CLEARED
    }

    fun isPaused(): Boolean {
        return status == Status.PAUSED
    }

    fun pause() {
        clear()
        status = Status.PAUSED

    }

    fun clear() {
        if (status == Status.CLEARED) return
        cancel()
        resources?.apply {
            releaseResources(this)
        }
        status = Status.CLEARED
    }

    private fun releaseResources(resources: Resources) {
        resources.release()
        this.resources = null
    }

    /**
     * 取消任务
     */
    private fun cancel() {
        viewTarget?.cancel()
        loadStatus?.apply {
            cancel()
            loadStatus = null
        }
    }


    private fun getErrorDrawable(): Drawable? {
        requestOptions?.apply {
            if (errDrawable == null && errorId > 0) {
                errDrawable = loadDrawable(errorId)
            }
            return errDrawable
        }
        return null
    }

    private fun getPlaceholderDrawable(): Drawable? {
        requestOptions?.apply {
            if (placeholderDrawable == null && placeholderId > 0) {
                placeholderDrawable = loadDrawable(placeholderId)
            }
            return placeholderDrawable
        }
        return null
    }


    private fun loadDrawable(resourceId: Int): Drawable? {
        glideContext?.apply {
            return ResourcesCompat.getDrawable(
                context.resources, resourceId, context.theme
            )
        }
        return null
    }

    private fun setErrorPlaceholder() {
        var error = getErrorDrawable()
        if (error == null) {
            error = getPlaceholderDrawable()
        }
        viewTarget?.onLoadFailed(error)
    }

    /**
     * 让Target计算完宽高后回调这里，正式开始图片的加载
     */
    override fun onSizeReady(width: Int, height: Int) {
        //切换执行状态  加载中
        status = Status.RUNNING
        //真正加载图片的功能并不是这里实现，而是交给Engine去做，Request来监听
        // TODO: 有安全隐患，glideContext和model有可能在执行的时候被挂起然后其它线程置为空，暂时先强行调用，后面解决
        loadStatus = engine?.load(
            glideContext!!,
            model!!,
            width,
            height,
            this
        )
    }

    /**
     * Engine加载完数据回来了，回调这里
     */
    override fun onResourceReady(reference: Resources?) {
        loadStatus = null
        resources = reference
        //如果回调的数据是空的，代表加载失败了，切换执行状态并显示占位图(如果有的话)
        if (resources == null) {
            status = Status.FAILED
            setErrorPlaceholder()
            return
        }
        //成功的话，就交给Target setBitmap。
        viewTarget?.onResourceReady(resources?.bitmap)
    }


}