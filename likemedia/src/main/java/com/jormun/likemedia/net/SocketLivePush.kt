package com.jormun.likemedia.net

import android.util.Log
import com.jormun.likemedia.codec.BaseEncoder
import com.jormun.likemedia.codec.H264Encoder
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import kotlin.Exception

/**
 * 投屏功能，发送端(投屏端)。
 * 默认的端口号是9007
 */
class SocketLivePush(private val socketCallback: SocketCallback? = null) {

    private val TAG = "SocketLive"

    private var webSocket: WebSocket? = null

    private lateinit var encoder: BaseEncoder

    // TODO: 默认端口号
    private var defaultPort = 9007

    private var isStart = false

    //private lateinit var webSocketServer: PushWebSocket


    fun start(encoder: BaseEncoder) {
        if (isStart) return
        synchronized(SocketLivePush::class.java) {
            if (isStart) return
            webSocketServer.start()
            Log.e(TAG, "start: ${webSocketServer.port}")
            this.encoder = encoder
            this.encoder.startEncoder()
            isStart = true
        }

    }

    /**
     * 发送数据。
     * @param bytes: 需要发送的数据。
     * @param type: 类型，0是视频1是音频。
     */
    fun sendData(bytes: ByteArray, type: Int) {
        synchronized(SocketLivePush::class.java) {
            //先打上标记区分是视频还是音频
            val adjustBuff = adjustType(bytes, type)
            try {
                /*if (webSocket == null) {
                    webSocketServer.start()
                }*/
                // Log.e(TAG, "sendData: ${bytes.contentToString()}")
                webSocket?.apply {
                    if (isOpen) {

                        send(adjustBuff)
                        Log.e(TAG, "sendData: ${bytes.contentToString()}")
                        //Log.e(TAG, "sendData: ${remoteSocketAddress.hostString}")

                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 往头部打上标记来区分是视频数据还说音频数据。
     */
    private fun adjustType(bytes: ByteArray, type: Int): ByteArray {
        val newBuf = ByteArray(bytes.size + 1)
        newBuf[0] = type.toByte()
        System.arraycopy(bytes, 0, newBuf, 1, bytes.size)
        return newBuf
    }

    private var webSocketServer = object : WebSocketServer(InetSocketAddress(defaultPort)) {
        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            this@SocketLivePush.webSocket = conn
            Log.e(TAG, "onOpen: socket push open")
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            Log.e(TAG, "onClose: socket push onClose")
        }

        override fun onMessage(conn: WebSocket?, message: String?) {

        }


        override fun onError(conn: WebSocket?, ex: Exception?) {
            Log.e("websocket", "->onError()出现异常：$ex")
            ex?.printStackTrace()
        }

        override fun onStart() {
            Log.e(TAG, "onStart: socket push onStart")
        }
    }

     fun socketClose() {
        if (!isStart) return
        synchronized(SocketLivePush::class.java) {
            if (!isStart) return
            webSocket?.apply {
                close()
                isStart = false
            }
        }
    }

}