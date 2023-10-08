package com.jormun.likemedia.net

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

class PushWebSocket(port: Int) : WebSocketServer(InetSocketAddress(port)) {

    private val TAG = "PushWebSocket"

    var webSocket: WebSocket? = null
        private set


    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        this.webSocket = conn
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