package com.example.temiapp.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.rabbitmq.client.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RabbitMQClient(
    private val connection: RabbitMQConnection,
    private val queues: Map<String, (String) -> Unit> // Map of queue names to their message handlers
) {

    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
    }

    private var connectionListener: ConnectionListener? = null

    fun setConnectionListener(listener: ConnectionListener) {
        this.connectionListener = listener
    }

    fun startConsuming() {
        val channel = connection.connect()
        if (channel != null) {
            connectionListener?.onConnected()

            // Setup consumers for all specified queues
            queues.forEach { (queueName, messageHandler) ->
                channel.queueDeclare(queueName, false, false, false, mapOf("x-message-ttl" to 300000))

                val consumer = object : DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String?,
                        envelope: Envelope?,
                        properties: AMQP.BasicProperties?,
                        body: ByteArray?
                    ) {
                        val message = String(body ?: ByteArray(0), StandardCharsets.UTF_8)
                        handleRabbitMqMessage(queueName, message)
                    }
                }

                channel.basicConsume(queueName, true, consumer)
                Log.d("RabbitMQClient", "Connected and consuming messages from $queueName")
            }
        } else {
            connectionListener?.onDisconnected()
        }
    }


    private fun handleRabbitMqMessage(queueName: String, message: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            queues[queueName]?.invoke(message)
        }
    }

    fun stopConsuming() {
        connection.disconnect()
        connectionListener?.onDisconnected()
    }
}
