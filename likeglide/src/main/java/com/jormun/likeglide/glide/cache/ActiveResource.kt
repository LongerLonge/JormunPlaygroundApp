package com.jormun.likeglide.glide.cache

import android.util.Log
import com.jormun.likeglide.glide.bean.Key
import com.jormun.likeglide.glide.bean.ResourceWeakRef
import com.jormun.likeglide.glide.bean.Resources
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Exception
import java.lang.ref.ReferenceQueue

/**
 * 活动缓存
 * 换句话说就是正在使用(活跃)的Bitmap存放地方。
 */

class ActiveResource(private val resourceListener: Resources.ResourceListener) {
    private val TAG = "ActiveResource"

    private val activeResourcesMap = mutableMapOf<Key, ResourceWeakRef>()
    private lateinit var resourceQueue: ReferenceQueue<Resources>
    private lateinit var watchResourceQueueJob: Job


    fun getActiveResource(key: Key?): Resources? {
        if (key != null) {
            return activeResourcesMap[key]?.get()
        }
        return null
    }

    /**
     * 对活动中的Bitmap进行缓存
     */
    fun activeResource(key: Key?, resources: Resources?) {
        if (key == null || resources == null) {
            Log.e(TAG, "activeResource: key or resource not null.")
            return
        }
        resources.setResourceListener(key, resourceListener)
        activeResourcesMap[key] =
            ResourceWeakRef(key, resources, getResourceQueue())
    }

    fun deactivateResource(key: Key?): Resources? {
        if (key != null) {
            val removedResource = activeResourcesMap.remove(key)
            removedResource?.apply {
                return removedResource.get()
            }
        }
        Log.e(TAG, "deactivateResource: key not null!")
        return null
    }

    @OptIn(DelicateCoroutinesApi::class)//加上这玩意来压制GlobalScope.launch的Delicate警告
    private fun getResourceQueue(): ReferenceQueue<Resources> {
        if (!this::resourceQueue.isInitialized) {
            resourceQueue = ReferenceQueue()
            //开启顶层协程来一直监听引用队列，如果触发了弱引用的回收就抛出去处理
            //在这个例子里面就是当活动缓存里面的Bitmap被移除了，就抛出去给内存缓存保存。
            watchResourceQueueJob = GlobalScope.launch(Dispatchers.IO) {
                //这个remove()方法是一个for(;;)死循环阻塞线程等待有回收才触发。
                //所以需要开启一个子线程(在kt里面就是协程了)来保证主线程不会被阻塞。
                try {
                    val removedResource = resourceQueue.remove() as ResourceWeakRef
                    activeResourcesMap.remove(removedResource.key)
                } catch (e: Exception) {
                    Log.e(TAG, "getResourceQueue: remove resource err. ${e.message}")
                }
            }
        }
        return resourceQueue
    }

    fun destroy() {
        watchResourceQueueJob.cancel()
    }


}