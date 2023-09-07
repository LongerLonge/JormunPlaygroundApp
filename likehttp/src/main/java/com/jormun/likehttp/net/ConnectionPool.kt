package com.jormun.likehttp.net

import java.util.ArrayDeque
import java.util.Deque
import java.util.Objects
import java.util.concurrent.Executor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 *
 * 可复用的连接池对象
 *
 * keep-alive 就是浏览器和服务端之间保持长连接，这个连接是可以复用的。在HTTP1.1中是默认开启的。
 *
 * 连接的复用为什么会提高性能呢？
 *
 *(一次响应的过程) 通常我们在发起http请求的时候首先要完成tcp的三次握手，然后传输数据，最后再释放连接
 *
 * 如果在高并发的请求连接情况下或者同个客户端多次频繁的请求操作，无限制的创建会导致性能低下。
 * 如果使用keep-alive，在timeout空闲时间内，连接不会关闭，相同重复的request将复用原先的connection，
 * 减少握手的次数，大幅提高效率。（并非keep-alive的timeout设置时间越长，就越能提升性能。
 * 长久不关闭会造成过多的僵尸连接和泄露连接出现）
 *
 * @param keepAliveDuration: 连接存活时间
 * @param timeUnit: 跟上面那个配套使用
 */
class ConnectionPool(private var keepAliveDuration: Long, timeUnit: TimeUnit) {

    companion object {
        /**
         *线程工厂，给线程池用的
         */
        private val threadFactory = ThreadFactory {
            val thread = Thread(it, "LikeHttp ConnectionPool")
            thread.isDaemon = true
            thread
        }

        /**
         * 线程池
         * 核心线程为0，意味着任何线程都只能存活一段时间
         */
        private val executor: Executor = ThreadPoolExecutor(
            0,//核心线程为0，意味着任何线程都只能存活一段时间
            Int.MAX_VALUE,//最大线程为无界，代表可以无限创建线程
            60L,//空闲线程存活时间
            TimeUnit.SECONDS,
            SynchronousQueue<Runnable>(),//这个队列是即时队列，来一个建一个(或者取)线程去跑
            threadFactory
        )
    }

    /**
     * 提供一个无参构造
     */
    constructor() : this(1, TimeUnit.MINUTES)


    private var cleanupRunning: Boolean = false

    private val connections: Deque<HttpConnection> = ArrayDeque()

    init {
        keepAliveDuration = timeUnit.toMillis(keepAliveDuration)
    }

    /**
     * 清理超时连接的子线程
     */
    private val cleanupRunnable = Runnable {
        while (true) {
            //清理超时连接，同时获取没超时的连接
            val waitTimes: Long = cleanUp(System.currentTimeMillis())
            //假如为-1代表池子没任何连接(初始化或者已经被清完了)，直接退出
            if (waitTimes == -1L) {
                break
            }
            //大于0代表有没超时的连接
            if (waitTimes > 0) {
                synchronized(this@ConnectionPool) {
                    //等待指定时间再去清理连接
                    try {
                        //调用某个对象的wait()方法能让当前线程阻塞，
                        // 并且当前线程必须拥有此对象的monitor（即锁）
                        //this@ConnectionPool.wait(waitTimes)，kt默认不实现wait方法暂时这样用
                        (this@ConnectionPool as Object).wait(waitTimes)
                    } catch (ignored: InterruptedException) {
                    }
                }
            }
        }
    }

    /**
     * 获取可复用的连接
     * @param host: 主机地址
     * @param port: 端口
     */
    fun get(host: String, port: Int): HttpConnection? {
        val iterator = connections.iterator()
        while (iterator.hasNext()) {
            val connection = iterator.next()
            //如果是同样的host和port，那么就直接返回吧
            if (connection.isSameAddress(host, port)) {
                //返回之前先移除
                iterator.remove()
                return connection
            }
        }
        return null
    }

    /**
     * 把连接加入连接池
     */
    fun put(httpConnection: HttpConnection) {
        //添加前先清理
        if (!cleanupRunning) {
            cleanupRunning = true
            executor.execute(cleanupRunnable)
        }
        connections.add(httpConnection)
    }

    /**
     * 清除超时连接，防止僵尸连接的存在
     * @param now: 当前时间
     * @return -1或者大于0的数，大于0的数代表有未超时连接
     */
    private fun cleanUp(now: Long): Long {
        var longestIdleDuration = -1L
        synchronized(this) {
            val it = connections.iterator()
            while (it.hasNext()) {
                val connection = it.next()
                //当前时间 - 上一次请求的时间
                val idleDuration = now - connection.lastUsetime
                //判断是否超出我们指定的存活时间，是的话就关闭掉
                if (idleDuration > keepAliveDuration) {
                    connection.closeQuietly()
                    it.remove()
                    continue
                }
                //得出所有连接中最长的一个存活时间
                if (longestIdleDuration < idleDuration) {
                    longestIdleDuration = idleDuration
                }
            }
            if (longestIdleDuration >= 0) {
                //指定的可存活时间 - 所有连接中最长的一个存活时间
                //也就是说算出当前连接池里面没有超出指定存活时间但是存活时间最长的那个连接时间
                return keepAliveDuration - longestIdleDuration
            } else {
                cleanupRunning = false
                return longestIdleDuration
            }
        }
    }

}