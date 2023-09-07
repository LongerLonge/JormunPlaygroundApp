package com.jormun.likehttp

import com.jormun.likehttp.chain.BridgeInterceptor
import com.jormun.likehttp.chain.CallServerInterceptor
import com.jormun.likehttp.chain.ConnectionInterceptor
import com.jormun.likehttp.chain.Interceptor
import com.jormun.likehttp.chain.RealInterceptorChain
import com.jormun.likehttp.chain.RetryInterceptor
import com.jormun.likehttp.net.HttpConnection
import com.jormun.likehttp.net.Request
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * call的实现类
 *@param request: 需要外界构建好Request信息才能创建Call
 * @param likeHttpClient: 客户端对象，提供运行环境
 */
class RealCall(private val request: Request, val likeHttpClient: LikeHttpClient) : Call {

    //是否执行，原子变量保证线程安全性
    private var executed = AtomicBoolean()

    //是否关闭
    @Volatile
    private var canceled = false


    override fun request(): Request {
        return request
    }

    /**
     * 同步执行获取response
     */
    override fun execute(): Response {
        try {
            //扔去Dispatcher里面标记为执行中
            likeHttpClient.dispatcher.executed(this)
            //直接调用获取response方法就可以了
            return getResponseWithInterceptorChain()
        } finally {
            //请求完毕，让Dispatcher标记已完成
            likeHttpClient.dispatcher.finished(this)
        }
    }


    /**
     * 经过一层层的拦截器获取response
     */
    fun getResponseWithInterceptorChain(): Response {
        val interceptors = mutableListOf<Interceptor>()
        //第一步添加用户自定义的应用层拦截器
        interceptors.addAll(likeHttpClient.interceptors)
        //重试拦截器
        interceptors.add(RetryInterceptor())
        //桥接拦截器，添加和解析请求头
        interceptors.add(BridgeInterceptor())
        //缓存拦截器，实现太复杂我们这里略过
        //interceptors.add(CacheInterceptor())
        //连接拦截器，创建和复用连接
        interceptors.add(ConnectionInterceptor())
        //请求拦截器，真正发起请求
        interceptors.add(CallServerInterceptor())

        //创建初始链
        val chain = RealInterceptorChain(interceptors, this, 0, request, null)

        //开始推进链和拦截器的运行
        return chain.proceed(request)
    }

    override fun enqueue(callback: Callback) {
        //check: 如果为true，则抛出异常，括号内为异常的message
        //compareAndSet: 线程安全情况下改变executed的值
        check(executed.compareAndSet(false, true)) {
            "Already Executed"
        }
        //丢给dispatcher异步执行
        likeHttpClient.dispatcher.enqueue(AsyncCall(callback))
    }

    override fun isCanceled(): Boolean = canceled
    override fun cancel() {
        canceled = true
    }

    /**
     * 在Kt里面
     * inner class是匿名内部类，持有外部类的引用。
     * inner class需要先创建外部类才能创建内部类或者使用内部类。
     * A().B()
     *
     * 普通内部类则是静态的，不持有外部类的引用。
     * 普通内部类不需要先创建外部类，直接调用即可(因为是静态)。
     * A.B()
     *
     * AsyncCall是异步的call执行者，也就是一个子线程
     *
     */
    inner class AsyncCall(private val callback: Callback) : Runnable {
        /**
         * 关键参数Host计数器
         * Volatile解决多线程可见性，AtomicInteger解决多线程安全性
         * private set 私有set也就是只允许这个类内部改变这个参数
         */
        @Volatile
        var callsPerHost = AtomicInteger(0)
            private set

        //host
        val host = request.url.host

        /**
         * 根据传入的Call，复写掉这个Call的Host计数。
         * @param otherCall: 同一个Host的Call
         */
        fun reuseHostCount(otherCall: AsyncCall) {
            callsPerHost = otherCall.callsPerHost
        }

        override fun run() {
            //防止多次通知，无论成功失败只通知一次
            var signalledCallback = false
            try {
                val response = getResponseWithInterceptorChain()
                signalledCallback = true
                if (canceled)
                    callback.onFailure(this@RealCall, IOException("Canceled!"))
                else
                    callback.onResponse(this@RealCall, response)
            } catch (e: IOException) {
                if (!signalledCallback) {
                    // Do not signal the callback twice!
                    callback.onFailure(this@RealCall, e)
                }
            } catch (t: Throwable) {
                cancel()
                if (!signalledCallback) {
                    callback.onFailure(this@RealCall, IOException("canceled due to $t"))
                }
            } finally {
                //完成请求后需要移除掉call(无论成功与否)
                likeHttpClient.dispatcher.finished(this)
            }
        }

    }


}