package com.jormun.likeshadow

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.util.Log
import com.jormun.likeshadow.exception.ApkPathNullException
import com.jormun.likeshadow.exception.HostContextNullException
import com.jormun.likeshadow.utils.FileUtils
import dalvik.system.DexClassLoader
import java.io.File
import kotlin.Exception

class PluginManagerImpl {
    companion object {
        //单例用了懒汉代码块同步锁式
        val sInstance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            PluginManagerImpl()
        }
        const val TAG = "PluginManagerImpl"
    }

    private var pluginApkDexClassLoader: DexClassLoader? = null
    private var hostContext: Context? = null
    private var apkPkgName: String = ""
    private var pluginApkResources: Resources? = null


    fun getPluginApkDexClassLoader(): DexClassLoader? {
        if (pluginApkDexClassLoader == null)
            throw HostContextNullException()
        return pluginApkDexClassLoader
    }

    fun getPluginApkResources(): Resources? {
//        if (pluginApkResources == null)
//            throw ResourcesNullException()
        return pluginApkResources
    }

    fun setHostContext(context: Context) {
        hostContext = context.applicationContext
    }

    //从apk中解析出启动的Intent
    fun getLaunchIntentFromApk(apkPath: String?): Intent? {
        if (apkPath.isNullOrEmpty())
            throw ApkPathNullException()
        if (hostContext == null)
            throw HostContextNullException()
        try {
            hostContext?.apply {
                val packageArchiveInfo = packageManager.getPackageArchiveInfo(apkPath, 0)
                packageArchiveInfo?.apply {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    return launchIntent
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 读取插件apk里面的文件，用里面的数据生成一个DexClassLoader
     * @param apkPath: 插件Apk的文件路径
     */
    fun loadPluginApkPath(apkPath: String?) {
        if (apkPath.isNullOrEmpty())
            throw ApkPathNullException()
        if (hostContext == null)
            throw HostContextNullException()
        try {
            hostContext?.apply {

                //1. 初始化ClassLoader
                val odex_dir = getDir("odex", Context.MODE_PRIVATE)
                /*pluginApkDexClassLoader =
                    DexClassLoader(apkPath, odex_dir.absolutePath, null, classLoader)*/
                BaseDexClassLoaderHookHelper.patchClassLoader(classLoader, File(apkPath), odex_dir)
                //2. 初始化resources
                /*val pluginAssets: AssetManager = AssetManager::class.java.newInstance()
                val addAssetPathMethod =
                    AssetManager::class.java.getMethod("addAssetPath", String::class.java)
                addAssetPathMethod.invoke(pluginAssets, apkPath)
                pluginApkResources =
                    Resources(pluginAssets, resources.displayMetrics, resources.configuration)*/
                pluginApkResources = createResources(apkPath)
                //开始拷贝so库：
                val isSuccess = copyAndLoadJniSO(apkPath)
                Log.e(TAG, "loadPluginApkPath: ")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "loadPluginApkPath: fail.")
        }
    }

    private fun copyAndLoadJniSO(apkPath: String): Any {
        var result: Boolean
        //          拷贝so文件到files/lib目录
        try {
            val toPath = hostContext?.filesDir?.absolutePath + File.separator + "plugin"
            result = FileUtils.unzipPack(apkPath, toPath, ".so")
            FileUtils.soList.reverse()
            for (soName in FileUtils.soList) {
                val soFile = File(toPath + File.separator + "lib/armeabi-v7a/$soName")
                if (soFile.exists()) {
                    Log.e(TAG, "copyAndLoadJniSO: ${soFile.absolutePath}")
                    System.load(soFile.absolutePath)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result = false
        }
        return result
    }

    fun createResources(apkPath: String): Resources? {
        hostContext?.apply {
            val pluginApplicationInfo = ApplicationInfo()
            pluginApplicationInfo.packageName = applicationInfo.packageName
            pluginApplicationInfo.uid = applicationInfo.uid

            fillApplicationInfoForNewerApi(pluginApplicationInfo, applicationInfo, apkPath)
            try {
                val resourcesForApplication =
                    packageManager.getResourcesForApplication(pluginApplicationInfo)
                return resourcesForApplication

            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException(e)
            }
        }
        return null
    }

    private fun fillApplicationInfoForNewerApi(
        applicationInfo: ApplicationInfo,
        hostApplicationInfo: ApplicationInfo,
        pluginApkPath: String
    ) {
        /**
         * 这里虽然sourceDir和sharedLibraryFiles中指定的apk都会进入Resources对象，
         * 但是只有资源id分区大于0x7f时才能在加载之后保持住资源id分区。
         * 如果把宿主的apk路径放到sharedLibraryFiles中，我们假设宿主资源id分区是0x7f，
         * 则加载后会变为一个随机的分区，如0x30。因此放入sharedLibraryFiles中的apk的
         * 资源id分区都需要改为0x80或更大的值。
         *
         * 考虑到现网可能已经有旧方案运行的宿主和插件，而宿主不易更新。
         * 因此新方案假设宿主保持0x7f固定不能修改，但是插件可以重新编译新版本修改资源id分区。
         * 因此把插件apk路径放到sharedLibraryFiles中。
         *
         * 复制宿主的sharedLibraryFiles，主要是为了获取前面WebView初始化时，
         * 系统使用私有API注入的webview.apk
         */
        applicationInfo.publicSourceDir = hostApplicationInfo.publicSourceDir
        applicationInfo.sourceDir = hostApplicationInfo.sourceDir

        // hostSharedLibraryFiles中可能有webview通过私有api注入的webview.apk
        val hostSharedLibraryFiles = hostApplicationInfo.sharedLibraryFiles
        val otherApksAddToResources =
            if (hostSharedLibraryFiles == null)
                arrayOf(pluginApkPath)
            else
                arrayOf(
                    *hostSharedLibraryFiles,
                    pluginApkPath
                )

        applicationInfo.sharedLibraryFiles = otherApksAddToResources
    }
}