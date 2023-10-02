package com.jormun.likemedia

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.jormun.likemedia.codec.CodeType
import com.jormun.likemedia.codec.H264Encoder
import com.jormun.likemedia.cons.VideoFormat
import com.jormun.likemedia.net.SocketLivePush
import com.jormun.likemedia.utils.UiUtils


class ScreenRecordService : Service() {
    private val TAG = "ScreenShortRecordService"

    // Binder given to clients
    private val binder = LocalBinder()

    private lateinit var socketLivePush: SocketLivePush

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): ScreenRecordService = this@ScreenRecordService
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, " onStartCommand intent = $intent")

        //启动Service
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        //创建前台提醒Service
        createNotificationChannel();

        var resultCode = -1
        var data: Intent? = null
        //do MediaProjection things that you want
        intent?.apply {
            extras?.apply {
                resultCode = getInt("code", -1)
            }
            data = getParcelableExtra<Intent>("data")
        }

        if (data != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
            isCanRecord = true
        }
        return binder
    }

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection

    private var isCanRecord = false


    fun startRecord(isStream: Boolean, codeType: CodeType) {
        val h264Encoder = H264Encoder(mediaProjection, VideoFormat.VIDEO_WIDTH, VideoFormat.VIDEO_HEIGHT, isStream, codeType)
        if (isStream) {
            if (!this::socketLivePush.isInitialized) {
                socketLivePush = SocketLivePush()
            }
            h264Encoder.setTheSocketLive(socketLivePush)
            socketLivePush.start(h264Encoder)
        } else
            h264Encoder.startEncoder()
    }

    /**
     * 设置前台提示，高版本适配
     */
    private fun createNotificationChannel() {
        //以下都是样板代码，就是创建一个notification来把这个Service变更为前台Service而已。

        val builder = Notification.Builder(this.applicationContext) //获取一个Notification构造器

        val nfIntent = Intent(this, RecordActivity::class.java) //点击后跳转的界面，可以设置跳转数据


        builder.setContentIntent(
            PendingIntent.getActivity(
                this, 0, nfIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        ) // 设置PendingIntent
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    this.resources,
                    R.mipmap.sym_def_app_icon
                )
            ) // 设置下拉列表中的图标(大图标)
            //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
            .setSmallIcon(R.mipmap.sym_def_app_icon) // 设置状态栏内的小图标
            .setContentText("is running......") // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setChannelId("notification_id")
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "notification_id",
                "notification_name",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = builder.build() // 获取构建好的Notification

        notification.defaults = Notification.DEFAULT_SOUND //设置为默认的声音

        startForeground(110, notification)
    }
}