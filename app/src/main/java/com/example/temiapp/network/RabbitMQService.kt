package com.example.temiapp.network

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.temiapp.controller.RobotController
import com.example.temiapp.ui.LoadingActivity
import com.robotemi.sdk.Robot


class RabbitMQService : Service() {

    private lateinit var rabbitMQClient: RabbitMQClient

    private val binder = RabbitBinder()
    private lateinit var robot: Robot

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
            "robot_control_queue" to { message: String -> handleRabbitMqMessageController(message) },
//            "store_update_queue" to { message: String -> handleRabbitMqMessage(message)}
        )

        rabbitMQClient = RabbitMQClient(
            host = "192.168.1.104",
            port = 5672,
            username = "admin",
            password = "123456",
            queues = queueHandlers
        )

        rabbitMQClient.connect() // Connect to RabbitMQ
    }

    override fun onDestroy() {
        super.onDestroy()
        rabbitMQClient.disconnect() // Disconnect when the service is destroyed
    }

    // Example of handling RabbitMQ messages in the service
    private fun handleRabbitMqMessageController(message: String) {
        Log.d("RabbitMQService", "Received message: $message")
        val robot = Robot.getInstance()
        val controller = RobotController(robot)
        controller.handleRabbitMqMessage(message)
    }


    // Method to stop RabbitMQ connection manually
    fun stopRabbitMQ() {
        rabbitMQClient.disconnect()
    }
}
