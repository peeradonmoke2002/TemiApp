package com.example.temiapp.network

import android.util.Log
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import okhttp3.Protocol

class RabbitMQConnection(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String
) {

    private var connection: Connection? = null
    private var channel: Channel? = null


    // Create a connection and channel
    fun connect(): Channel? {
        return try {
            val factory = ConnectionFactory().apply {
                this.host = this@RabbitMQConnection.host
                this.port = this@RabbitMQConnection.port
                this.username = this@RabbitMQConnection.username
                this.password = this@RabbitMQConnection.password
                this.connectionTimeout = 30000
                this.requestedHeartbeat = 10
            }
            connection = factory.newConnection()
            channel = connection?.createChannel()
            Log.d("RabbitMQConnection", "Connected to RabbitMQ at $host:$port")
            channel
        } catch (e: Exception) {
            Log.e("RabbitMQConnection", "Failed to connect to RabbitMQ: ${e.message}", e)
            null
        }
    }

    // Cleanly disconnect from RabbitMQ
    fun disconnect() {
        try {
            channel?.takeIf { it.isOpen }?.close()
            connection?.takeIf { it.isOpen }?.close()
            Log.d("RabbitMQConnection", "Disconnected from RabbitMQ")
        } catch (e: Exception) {
            Log.e("RabbitMQConnection", "Error disconnecting from RabbitMQ: ${e.message}", e)
        }
    }
}
