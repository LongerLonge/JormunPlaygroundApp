package com.jormun.likeglide.glide.request

import com.jormun.likeglide.glide.cache.take.Util
import java.util.Collections
import java.util.WeakHashMap

/**
 * 对Request任务进行记录并且保存的类。
 * 根据不同情况可以管理任务列表，比如开启，暂停，清除等等。
 */
class RequestTrack {

    //加载任务的列表，用的是弱引用列表。
    private val requests = Collections.newSetFromMap(WeakHashMap<Request, Boolean>())

    //因为是弱引用，所以暂停了的任务可能会被回收，所以就需要加入到一个集合里面保证不会被回收。
    private val pendingRequests = mutableListOf<Request>()

    //是否暂停
    private var isPaused = false

    /**
     * 添加并让Request执行（允许运行的情况下）
     */
    fun runRequest(request: Request) {
        requests.add(request)
        if (!isPaused) {
            request.begin()
        } else {
            pendingRequests.add(request)
        }
    }

    fun pauseRequests() {
        isPaused = true
        Util.getSnapshot(requests).forEach { request ->
            if (request.isRunning()) {
                request.pause()
                pendingRequests.add(request)
            }
        }
    }
    /**
     * 让队列中所有Request执行
     */
    fun resumeRequests() {
        isPaused = false
        for (request in Util.getSnapshot(requests)) {
            if (!request.isCompleted() && !request.isCancelled() && !request.isRunning()) {
                request.begin()
            }
        }
        pendingRequests.clear()
    }

    fun clearRequests() {
        for (request in Util.getSnapshot(requests)) {
            if (request == null) return
            requests.remove(request)
            request.clear()
            request.recycle()
        }
        pendingRequests.clear()
    }
}