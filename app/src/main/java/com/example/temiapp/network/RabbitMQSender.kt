package com.example.temiapp.network

import android.util.Log
import java.nio.charset.StandardCharsets

class RabbitMQSender(
    private val connection: RabbitMQConnection
) {

    fun sendMessage(queueName: String, message: String) {
        val channel = connection.connect()
        try {
            channel?.basicPublish("", queueName, null, message.toByteArray(StandardCharsets.UTF_8))
            Log.d("RabbitMQSender", "Message sent to $queueName: $message")
        } catch (e: Exception) {
            Log.e("RabbitMQSender", "Error sending message to $queueName: ${e.message}", e)
        } finally {
            // Close the channel if only used for sending messages
            channel?.close()
        }
    }
}
