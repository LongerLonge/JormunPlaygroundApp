package com.jormun.likemedia.ui.view

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.jormun.likemedia.CameraActivity
import com.jormun.likemedia.codec.H264RecordCodec
import com.jormun.likemedia.net.SocketLivePush


class LocalSurfaceView : SurfaceView, SurfaceHolder.Callback,
    Camera.PreviewCallback {


    //被弃用，这里暂时先用这个学习
    private lateinit var mCamera: Camera
    private lateinit var cSize: Camera.Size
    private lateinit var buffer: ByteArray
    private lateinit var h264RecordCodec: H264RecordCodec
    var isStream: Boolean = false
    private lateinit var socketLivePush: SocketLivePush


    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        holder.addCallback(this)

    }

    /* init {
         holder.addCallback(this)
     }*/

    private fun startPreview() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        val parameters = mCamera.parameters
        cSize = parameters.previewSize
        try {
            mCamera.setPreviewDisplay(holder)
            mCamera.setDisplayOrientation(90)
            //单个yuv像素为1.5字节，宽高乘以1.5(3/2)字节便是整个显示区域所需大小。
            buffer = ByteArray(cSize.width * cSize.height * 3 / 2)
            mCamera.addCallbackBuffer(buffer)
            mCamera.setPreviewCallbackWithBuffer(this)
            mCamera.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    //回调接口，Camera捕获到的数据会通过这个接口回传
    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        //data是摄像头捕获到的yuv数据
        /*val cameraActivity = context as CameraActivity
        cameraActivity.encodeFrame(data,cSize.width, cSize.height)*/
        if (!this::h264RecordCodec.isInitialized) {
            // h264RecordCodec = H264RecordCodec(cSize.width, cSize.height)
            //创建相机捕获编码器时，不能自己指定宽高，而是以相机当前的宽高信息为准。
            h264RecordCodec = H264RecordCodec(cSize.width, cSize.height, isStream)
            //h264RecordCodec = H264RecordCodec(isStream = isStream)
        }
        if (!isStream) {
            h264RecordCodec.startEncoder()
        } else {
            //如果是推流就开启Socket推流
            h264RecordCodec.setTheSocketLive(socketLivePush)
            socketLivePush.start(h264RecordCodec)
        }
        h264RecordCodec.encodeFrame(data)

        mCamera.addCallbackBuffer(data)
    }

    fun getSocketLivePush(): SocketLivePush {
        return h264RecordCodec.getTheSocketLive()
    }

    fun setSocketLivePush(socketLivePush: SocketLivePush) {
        this.socketLivePush = socketLivePush
    }

}