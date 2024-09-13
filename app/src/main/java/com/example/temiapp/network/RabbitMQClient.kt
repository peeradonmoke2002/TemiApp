package com.example.temiapp.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.rabbitmq.client.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RabbitMQClient(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val queueName: String,
    private val messageHandler: (String) -> Unit // Callback function to handle messages
) {

    private lateinit var connectionFactory: ConnectionFactory
    private lateinit var connection: Connection
    private lateinit var channel: Channel
    private lateinit var executor: ExecutorService

    fun connect() {
        executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                Log.d("RabbitMQClient", "Attempting to connect to RabbitMQ at $host:$port with user $username")

                connectionFactory = ConnectionFactory().apply {
                    this.host = this@RabbitMQClient.host
                    this.port = this@RabbitMQClient.port
                    this.username = this@RabbitMQClient.username
                    this.password = this@RabbitMQClient.password
                }

                connection = connectionFactory.newConnection()
                if (connection.isOpen) {
                    Log.d("RabbitMQClient", "Connection established successfully.")
                } else {
                    Log.e("RabbitMQClient", "Connection failed to open.")
                    return@execute
                }

                channel = connection.createChannel()

                // Declare the queue with appropriate configurations
                channel.queueDeclare(queueName, false, false, false, mapOf("x-message-ttl" to 300000))

                val consumer = object : DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String?,
                        envelope: Envelope?,
                        properties: AMQP.BasicProperties?,
                        body: ByteArray?
                    ) {
                        val message = String(body ?: ByteArray(0), StandardCharsets.UTF_8)
                        handleRabbitMqMessage(message)
                    }
                }

                // Consume messages from the queue
                channel.basicConsume(queueName, true, consumer)
                Log.d("RabbitMQClient", "Connected and consuming messages from $queueName")

            } catch (e: Exception) {
                Log.e("RabbitMQClient", "Error connecting to RabbitMQ: ${e.message}", e)
            }
        }
    }

    private fun handleRabbitMqMessage(message: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            messageHandler(message) // Call the provided handler function with the message
        }
    }

    fun disconnect() {
        try {
            if (::channel.isInitialized && channel.isOpen) {
                channel.close()
            }
            if (::connection.isInitialized && connection.isOpen) {
                connection.close()
            }
            Log.d("RabbitMQClient", "Disconnected from RabbitMQ")
        } catch (e: Exception) {
            Log.e("RabbitMQClient", "Error disconnecting from RabbitMQ: ${e.message}", e)
        } finally {
            executor.shutdownNow()
        }
    }
}