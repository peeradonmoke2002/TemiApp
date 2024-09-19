package com.example.temiapp.network

import android.util.Log
import org.java_websocket.server.WebSocketServer
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import java.net.InetSocketAddress

class WebSocketServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {

    private val clients = mutableListOf<WebSocket>()
    var isRunning: Boolean = false
        private set

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake?) {
        clients.add(conn)
        Log.d("WebSocketServer", "New connection from ${conn.remoteSocketAddress}")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
        clients.remove(conn)
        Log.d("WebSocketServer", "Connection closed: ${conn.remoteSocketAddress}")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        Log.d("WebSocketServer", "Message received: $message")
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        Log.e("WebSocketServer", "WebSocket error: ${ex.message}")
    }

    override fun onStart() {
        isRunning = true
        Log.d("WebSocketServer", "WebSocket server started on port $port")
    }

    fun broadcastFrame(frameData: ByteArray) {
        for (client in clients) {
            if (client.isOpen) {
                client.send(frameData)
            }
        }
    }

    override fun stop() {
        super.stop()
        isRunning = false
        println("WebSocket server stopped")
    }


    fun startServer() {
        this.start()
        Log.d("WebSocketServer", "WebSocket server is running")
    }

    fun stopServer() {
        this.stop()
        Log.d("WebSocketServer", "WebSocket server stopped")
    }

    fun isAlive(): Boolean {
        return isRunning
    }
}
