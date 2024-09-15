package com.example.temiapp.network

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.temiapp.controller.RobotController
import com.robotemi.sdk.Robot

class RabbitMQService : Service() {

    private lateinit var rabbitMQClient: RabbitMQClient
    private val binder = RabbitBinder()

    // Binder class to return the instance of RabbitMQService
    inner class RabbitBinder : Binder() {
        fun getService(): RabbitMQService = this@RabbitMQService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize RabbitMQ client with queue handlers
        val queueHandlers = mapOf(
            "robot_control_queue" to { message: String -> handleRabbitMqMessage(message) }
        )

        rabbitMQClient = RabbitMQClient(
            host = "your_host",
            port = 5672,
            username = "your_username",
            password = "your_password",
            queues = queueHandlers
        )

        rabbitMQClient.connect() // Connect to RabbitMQ
    }

    override fun onDestroy() {
        super.onDestroy()
        rabbitMQClient.disconnect() // Disconnect when the service is destroyed
    }

    // Example of handling RabbitMQ messages in the service
    private fun handleRabbitMqMessage(message: String) {
        Log.d("RabbitMQService", "Received message: $message")
        // Process the message (e.g., controlling the robot via RobotController)
        val robot = Robot.getInstance()
        val controller = RobotController(robot)
        controller.handleRabbitMqMessage(message)
    }

    // Method to stop RabbitMQ connection manually
    fun stopRabbitMQ() {
        rabbitMQClient.disconnect()
    }
}
