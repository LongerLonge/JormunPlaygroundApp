package com.jormun.likeglide.glide.load

import java.lang.Integer.min
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * 给图片加载执行用的线程池
 */
object GlideExecutor {
    //因为用到这个线程池的都是IO密集型任务，线程数量以核心数为准，最低是4个
    private var bestThreadCount: Int = max(4, Runtime.getRuntime().availableProcessors()) + 1
    //var bestThreadCount: Int = min(4, Runtime.getRuntime().availableProcessors())

    /**
     * 线程生产工厂，给线程池创建线程用的
     */
    private class DefaultThreadFactory : ThreadFactory {
        private var threadNum = 0
        override fun newThread(r: Runnable?): Thread {
            val thread = Thread(r, "likeglie-thread-${threadNum}")
            threadNum++
            return thread
        }
    }

    /**
     * 获得线程池执行器
     *
     */
    fun newExecutor(): ThreadPoolExecutor {
        /**
         * LinkedBlockingDeque：阻塞队列
         * 简单点说有两种情况
         * 1.取出里面元素的时候，如果队列为空，那么就阻塞住当前执行的代码(线程)，直到有数据进来为止。
         * 2.往队列添加元素时，如果已经满了，那么就阻塞住当前执行的代码(线程)直到有空间放进去为止。
         * 但是因为默认构造方法为Integer.MAX，可以理解为无界队列(也可以传个数进去作为容量限制), 因为是无界的所以
         * 默认情况下put任务进去的时候不会发生阻塞。
         */
        return ThreadPoolExecutor(
            bestThreadCount /* corePoolSize */,
            bestThreadCount /* maximumPoolSize */,
            0,
            TimeUnit.MILLISECONDS,
            LinkedBlockingDeque<Runnable>()/* 阻塞队列 */,
            DefaultThreadFactory()
        )
    }
}