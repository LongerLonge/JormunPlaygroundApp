package com.jormun.likemedia.net

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer
import kotlin.Exception

/**
 * 投屏功能，接收端
 * 采用Socket的 p2p连接，直连到投屏端。
 * 需要知道投屏端的ip地址和端口口，且必须在同一局域网。
 * 功能比较单一，只作学习演示用。
 */
class SocketLiveClient(
    private var socketCallback: SocketCallback,
    private var host: String = "192.168.31.60",
    private var port: Int = -1
) {
    private val TAG = "SocketLiveClient"
    private lateinit var myWebSocketClient: MyWebSocketClient
    private lateinit var webSocket: WebSocket
    private var isStart = false


    fun start() {
        if (isStart) return
        synchronized(SocketLiveClient::class.java) {
            try {
                //端口默认给9007
                if (port == -1) port = 9007
                //接收端需要连接到投屏端，ip地址看投屏端，比如小米在wifi信息里面可以看到
                val uri = URI("ws://${host}:${port}")
                // val uri = URI("ws://192.168.31.120:${port}")
                //val uri = URI("ws://192.168.232.2:${port}")
                myWebSocketClient = MyWebSocketClient(uri, socketCallback)
                myWebSocketClient.connect()
                isStart = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }


    private class MyWebSocketClient(serverURI: URI, private val socketCallback: SocketCallback) :
        WebSocketClient(serverURI) {
        private val TAG = "MyWebSocketClient"
        override fun onOpen(handshakedata: ServerHandshake?) {
            Log.i(TAG, "打开 socket  onOpen: ")

        }

        override fun onMessage(message: String?) {

        }

        override fun onMessage(bytes: ByteBuffer?) {
            bytes?.apply {
                Log.i(TAG, "消息长度  : " + bytes.remaining())
                val frameBuffer = ByteArray(bytes.remaining())
                bytes.get(frameBuffer)
                socketCallback.callback(frameBuffer)
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            Log.i(TAG, "onClose: $reason")
        }

        override fun onError(ex: Exception) {
            ex.printStackTrace()
            Log.i(TAG, "onError: ${ex.message}")
        }

    }

    fun socketClose() {
        if (!isStart) return
        synchronized(SocketLivePush::class.java) {
            if (!isStart) return
            myWebSocketClient.close()
            isStart = false
        }
    }
}