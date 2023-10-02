package com.jormun.likemedia.ui.view

import android.content.Context
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.jormun.likemedia.codec.H264RecordCodec


class LocalSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback,
    Camera.PreviewCallback {
    //被弃用，这里暂时先用这个学习
    private lateinit var mCamera: Camera
    private lateinit var cSize: Camera.Size
    private lateinit var buffer: ByteArray
    private lateinit var h264RecordCodec: H264RecordCodec

    init {
        holder.addCallback(this)
    }

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
        if (!this::h264RecordCodec.isInitialized) {
            h264RecordCodec = H264RecordCodec(cSize.width, cSize.height)
            h264RecordCodec.startLive()
        }
        h264RecordCodec.encodeFrame(data)
        mCamera.addCallbackBuffer(data)
    }

}