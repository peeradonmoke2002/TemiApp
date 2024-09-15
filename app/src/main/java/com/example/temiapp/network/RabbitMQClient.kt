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
    private val queues: Map<String, (String) -> Unit> // Map of queue names to their message handlers
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
                channel = connection.createChannel()

                // Declare queues and set up consumers for each queue
                queues.forEach { (queueName, messageHandler) ->
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
                            handleRabbitMqMessage(queueName, message)
                        }
                    }

                    // Consume messages from the queue
                    channel.basicConsume(queueName, true, consumer)
                    Log.d("RabbitMQClient", "Connected and consuming messages from $queueName")
                }

            } catch (e: Exception) {
                Log.e("RabbitMQClient", "Error connecting to RabbitMQ: ${e.message}", e)
            }
        }
    }

    private fun handleRabbitMqMessage(queueName: String, message: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            // Call the appropriate message handler for the given queue
            queues[queueName]?.invoke(message)
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
