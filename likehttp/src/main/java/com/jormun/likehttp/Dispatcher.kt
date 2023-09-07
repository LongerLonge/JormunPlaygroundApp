package com.jormun.likehttp

import com.jormun.likehttp.RealCall.AsyncCall
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 分发器，分发正在执行和准备执行的任务
 * 同时推进这些任务的执行
 * @param maxRequests 最多同时请求数量
 * @param maxRequestsPerHost 同一个Host(主机)最大请求数
 */
class Dispatcher(
    private var maxRequests: Int = 64,
    private var maxRequestsPerHost: Int = 2
) {
    //线程池对象，lazy获取并且要求线程安全（只会被初始化一次）
    private val executorService: ExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getTheExecutorService()
    }

    //等待执行的异步队列
    private val readyAsyncCalls = ArrayDeque<AsyncCall>()

    //正在执行的异步队列
    private val runningAsyncCalls = ArrayDeque<AsyncCall>()

    //正在执行的同步队列
    private val runningSyncCalls = ArrayDeque<RealCall>()

    private fun getTheExecutorService(): ExecutorService {
        val threadFactory = ThreadFactory { r -> Thread(r, "LikeHttp Dispatcher") }
        /**
         * corePoolSize: 核心线程数量(不会被回收)
         * maximumPoolSize: 最多可以创建多少个线程
         * keepAliveTime: 线程存活时间(不影响核心线程)
         * workQueue: 等待队列，决定元素如何进行入队和出队
         * threadFactory: 线程工厂，线程由这个工厂创建
         */
        /**
         * SynchronousQueue: BlockingQueue的子类，说明它也是阻塞队列，
         * BlockingQueue是get、put发现为空都会阻塞。
         * 也就是说假如某个线程put一个任务进去，发现没有空闲线程可以执行，就会阻塞(哪怕它是主线程)。
         * 线程池去队列里面get任务执行，发现为空，就阻塞。
         * 可以看作是put和get线程的状态交换，put跟get必须配对才能不阻塞。
         * 但是因为默认是无界队列，所以理论上不会被阻塞，put一个任务就创一个线程(假如没有空闲线程的话)。
         * 所以这个队列可以理解为就是来一个就立刻执行一个的无界队列。
         */
        return ThreadPoolExecutor(
            0,
            Integer.MAX_VALUE,
            60,
            TimeUnit.SECONDS,
            SynchronousQueue<Runnable>(),
            threadFactory
        )
    }

    /**
     * 同步请求方法，也就是阻塞式请求
     * 直接扔到运行队列里面去，只是作为记录用
     */
    @Synchronized
    fun executed(call: RealCall) {
        runningSyncCalls.add(call)
    }


    /**
     * 异步执行网络请求
     */
    fun enqueue(call: AsyncCall) {

        synchronized(this) {
            readyAsyncCalls.add(call)
            //传统方法判断最大请求和最大host
            /*if (runningAsyncCalls.size < maxRequests && runningCallsForHost(call) < maxRequestsPerHost) {
                executorService.execute(call)
            }*/
            //新版牛逼的用原子计数方法
            //在执行和等待列表里面找到同一个Host的Call
            val sameHostCall = findExistingCallWithHost(call.host)
            if (sameHostCall != null) {
                //然后复写掉当前这个Call的Host计数器
                //这样的话，就代表整个队列里面的所有同Host的Call都共用一个计数器
                //一旦某个同Host的Call的计数器发生改变，整个列表的所有同Host的Call都跟着变
                //因为这个计数器是原子变量且被@Volatile修饰，一旦修改所有线程可见并同步
                call.reuseHostCount(sameHostCall)
            }
        }
        promoteAndExecute()
    }

    /**
     * 遍历Call列表，符合条件的情况下就执行这些Call
     */
    private fun promoteAndExecute(): Boolean {
        //保存符合条件可执行的Call
        val executableCalls = mutableListOf<AsyncCall>()
        //返回标记，是否已经在运行
        val isRunning: Boolean
        //锁住，this代表锁住某个实例的所有synchronized代码
        synchronized(this) {
            //迭代器防止多线程修改列表，readyAsyncCalls可以被上面的enqueue添加修改
            val iterator = readyAsyncCalls.iterator()
            while (iterator.hasNext()) {//while遍历
                val nextCall = iterator.next()
                //当前执行的Call数量超出最大可执行数量就直接结束
                if (runningAsyncCalls.size >= maxRequests) break
                //通个Host有最大请求数量限制
                if (nextCall.callsPerHost.get() >= maxRequestsPerHost) break
                //都满足，先移除掉防止重复遍历
                iterator.remove()
                //host计数+1
                nextCall.callsPerHost.incrementAndGet()
                //因为都满足条件就直接加入到列表里面
                executableCalls.add(nextCall)
                //这个列表也要同步一下
                runningAsyncCalls.add(nextCall)
            }
            //如果size大于0则代表有任务正在执行，返回true
            isRunning = runningAsyncCalls.size > 0
        }

        if (executorService.isShutdown) {
            // TODO: 如果这个线程池被干掉了需要做一些处理，暂时略过
        } else {
            //扔到线程池里面执行
            for (executableCall in executableCalls) {
                executorService.execute(executableCall)
            }
        }
        return isRunning
    }

    /**
     * 结束异步请求
     */
    fun finished(call: AsyncCall) {
        //某个Call结束了请求，那么Host计数要记得减1(该请求可能会被再次发起)
        //一旦减去，整个列表的同Host的Call都会跟着一起减
        call.callsPerHost.decrementAndGet()
        finished(runningAsyncCalls, call)
    }

    /**
     * 结束同步请求
     */
    fun finished(call: RealCall) {
        finished(runningSyncCalls, call)
    }

    /**
     * 从队列中移除某个call
     * 通常就是执行完毕后把这个call从队列移除掉
     * @param calls: 队列
     * @param call: 需要移除的call
     */
    private fun <T> finished(calls: ArrayDeque<T>, call: T) {
        //一个call结束了，无论成功与否，都要从正在执行队列中，移除这个call，
        synchronized(this) {
            calls.remove(call)
        }
        //一个请求结束后，要主动推动让剩下的请求继续执行
        promoteAndExecute()

    }

    /**
     * 新版方法，通过遍历两个列表里面看看有没有同样的host的Call
     * 有的话就直接返回并且使用它的计数器。
     */
    private fun findExistingCallWithHost(host: String): AsyncCall? {
        for (runningAsyncCall in runningAsyncCalls) {
            if (host == runningAsyncCall.host) {
                return runningAsyncCall
            }
        }

        for (readyCall in readyAsyncCalls) {
            if (host == readyCall.host) {
                return readyCall
            }
        }
        return null
    }


    /**
     * 传统方法，遍历正在执行的Call列表
     * 找到一样的host数量
     */
    private fun runningCallsForHost(call: AsyncCall): Int {
        var result = 0
        for (runningAsyncCall in runningAsyncCalls) {
            if (call.host == runningAsyncCall.host) result++
        }
        return result
    }

}