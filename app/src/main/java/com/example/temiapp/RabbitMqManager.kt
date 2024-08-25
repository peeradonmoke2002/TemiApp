package com.example.temiapp

import android.util.Log
import com.rabbitmq.client.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RabbitMqManager(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val queueName: String
) {
    private lateinit var connectionFactory: ConnectionFactory
    private lateinit var connection: Connection
    private lateinit var channel: Channel
    private lateinit var executor: ExecutorService

    // Interface for handling incoming messages
    interface MessageHandler {
        fun onMessageReceived(message: String)
    }

    private var messageHandler: MessageHandler? = null

    fun setMessageHandler(handler: MessageHandler) {
        this.messageHandler = handler
    }

    fun connect() {
        executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                connectionFactory = ConnectionFactory().apply {
                    this.host = this@RabbitMqManager.host
                    this.port = this@RabbitMqManager.port
                    this.username = this@RabbitMqManager.username
                    this.password = this@RabbitMqManager.password
                }

                connection = connectionFactory.newConnection()
                channel = connection.createChannel()

                channel.queueDeclare(queueName, false, false, false, null)

                val consumer = object : DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String?,
                        envelope: Envelope?,
                        properties: AMQP.BasicProperties?,
                        body: ByteArray?
                    ) {
                        val message = String(body ?: ByteArray(0), StandardCharsets.UTF_8)
                        messageHandler?.onMessageReceived(message)
                    }
                }

                channel.basicConsume(queueName, true, consumer)
                Log.d("RabbitMqManager", "Connected and consuming messages from $queueName")

            } catch (e: Exception) {
                Log.e("RabbitMqManager", "Error connecting to RabbitMQ: ${e.message}")
            }
        }
    }

    fun disconnect() {
        try {
            channel.close()
            connection.close()
            executor.shutdownNow()
            Log.d("RabbitMqManager", "Disconnected from RabbitMQ")
        } catch (e: Exception) {
            Log.e("RabbitMqManager", "Error disconnecting from RabbitMQ: ${e.message}")
        }
    }
}
