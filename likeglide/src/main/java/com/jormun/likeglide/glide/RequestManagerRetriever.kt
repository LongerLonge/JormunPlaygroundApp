package com.jormun.likeglide.glide

import android.app.Activity
import android.app.Application
import android.app.Fragment
import android.app.FragmentManager
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.jormun.likeglide.glide.manager.ApplicationLifecycleManager
import com.jormun.likeglide.glide.manager.LifecycleLifecycleManager

/**
 * 帮忙初始化RequestManager
 * 初始化的同时，协助RequestManager注册到正确的生命周期监听管理类中去。
 * 这样就可以让RequestManager正确的与LifecycleListener进行生命周期的同步。
 * Retriever意为寻回犬。
 */
class RequestManagerRetriever {

    private val FRAGMENT_TAG = "glide_fragment"

    private val fragmentMap = HashMap<FragmentManager, LifecycleHelperFragment>()

    //因为applicationRequestManager只需要一个，所以直接用懒汉同步式来创建就好了
    private val applicationRequestManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getApplicationManager()
    }

    var glideContext: GlideContext? = null


    /**
     * 对Context进行分发判断
     */
    fun get(context: Context): RequestManager {
        if (context !is Application) {
            when (context) {
                is FragmentActivity -> {
                    return get(context)
                }

                is Activity -> {
                    return get(context)
                }

                is ContextWrapper -> {
                    return get(context.baseContext)
                }
            }
        }
        return applicationRequestManager
    }

    /**
     * 采用jetpack 的Lifecycle 监听。
     */
    fun get(fragmentActivity: FragmentActivity): RequestManager {
        val lifecycleLifecycleManager = LifecycleLifecycleManager(fragmentActivity.lifecycle)
        return RequestManager(lifecycleLifecycleManager)
    }

    /**
     * 最古老的Activity，通过塞入Fragment来监听
     * 新版本这种方式已废弃，采用直接绑定到Application上(摆烂)
     */
    fun get(activity: Activity): RequestManager {
        //旧版本，通过塞入Fragment来绑定
        //新版本采用这种方式了
        //return get(activity.applicationContext)
        return fragmentGet(activity)
    }

    /**
     * 作为彩蛋，通过View方式绑定也是塞入Fragment来绑定
     * Glide新版本也是这样做
     */
    fun get(view: View): RequestManager {
        //找到上一层的Activity
        val findActivity = findActivity(view.context)
        findActivity?.apply {
            if (findActivity is FragmentActivity) {
                return get(this)
            }
            return get(this)//找到直接用旧方法塞进去Fragment监听就行了
        }
        return get(view.context.applicationContext)//找不到的话就与Application同步吧
    }

    /**
     * 递归直到找到Activity
     */
    private fun findActivity(context: Context): Activity? {
        when (context) {
            is Activity -> return context
            is ContextWrapper -> return findActivity(context.baseContext)
        }
        return null
    }

    /**
     * 直接与app进程同步，等于啥也没干
     */
    private fun getApplicationManager(): RequestManager {
        val applicationLifecycleManager = ApplicationLifecycleManager()
        return RequestManager(applicationLifecycleManager)
    }

    /**
     * 返回RequestManager
     * 1、取出或者创建Fragment
     * 2、把RequestManager和Fragment互相绑定
     */
    private fun fragmentGet(activity: Activity): RequestManager {
        //获取Fragment
        val lifecycleHelperFragment = getRequestManagerFragment(activity.fragmentManager)
        //检查这个Fragment是否已经绑定了RequestManager
        var requestManager = lifecycleHelperFragment.getRequestManager()
        //没有就进行绑定
        if (requestManager == null) {
            //新建RequestManager的同时让RequestManager把自身注册到监听者列表里
            requestManager = RequestManager(lifecycleHelperFragment.fragmentLifecycleManager)
            //虽然set进去了，但是实际上只是作为标记用，在里面没用到它
            lifecycleHelperFragment.setRequestManager(requestManager)
        }
        return requestManager
    }

    /**
     * 创建Fragment并且塞入到Activity中。
     */
    private fun getRequestManagerFragment(fragmentManager: FragmentManager): LifecycleHelperFragment {
        //从fm中尝试寻找
        val findFragmentByTag = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (findFragmentByTag != null) {
            return findFragmentByTag as LifecycleHelperFragment
        }
        //有缓存机制，注意
        val cacheFragment = fragmentMap[fragmentManager]
        if (cacheFragment != null) {
            return cacheFragment
        }
        //都没有，就新建
        val fragment = LifecycleHelperFragment()
        fragmentMap[fragmentManager] = fragment
        fragmentManager.beginTransaction().add(fragment, FRAGMENT_TAG)
            .commitAllowingStateLoss()
        return fragment
    }

}