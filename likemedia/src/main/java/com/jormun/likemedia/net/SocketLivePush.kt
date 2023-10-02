package com.jormun.likemedia.net

import android.util.Log
import com.jormun.likemedia.codec.H264Encoder
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

/**
 * 投屏功能，发送端(投屏端)。
 * 默认的端口号是9007
 */
class SocketLivePush() {

    private val TAG = "SocketLive"

    private var webSocket: WebSocket? = null

    private lateinit var h264Encoder: H264Encoder

    // TODO: 默认端口号
    private var defaultPort = 9007

    fun start(h264Encoder: H264Encoder) {
        webSocketServer.start()
        this.h264Encoder = h264Encoder
        this.h264Encoder.startEncoder()
    }

    fun sendData(bytes: ByteArray) {

        webSocket?.apply {
            if (isOpen) {
                send(bytes)
                Log.e(TAG, "sendData: ${remoteSocketAddress.hostString}")
            }
        }
    }

    private val webSocketServer = object : WebSocketServer(InetSocketAddress(defaultPort)) {
        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            this@SocketLivePush.webSocket = conn
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {

        }

        override fun onMessage(conn: WebSocket?, message: String?) {

        }

        override fun onError(conn: WebSocket?, ex: Exception?) {

        }

        override fun onStart() {

        }
    }

}