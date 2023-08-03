package com.jormun.likeglide.glide.request

/**
 * 图片加载任务的一些参数。
 */
class RequestOptions {
     var errorId = 0//加载错误时，显示的图片ID
     var placeholderId = 0//占位图ID
    var overrideHeight = -1//指定高度
    var overrideWidth = -1//指定宽度

    fun errorId(errSourceId: Int): RequestOptions {
        errorId = errSourceId
        return this
    }

    fun placeholder(placeholderId: Int): RequestOptions {
        this.placeholderId = placeholderId
        return this
    }
}