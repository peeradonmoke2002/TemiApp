package com.example.temiapp.network

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.temiapp.controller.RobotController
import com.example.temiapp.data.ProductRepository
import com.robotemi.sdk.Robot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import com.example.temiapp.config.Config

class RabbitMQService : Service() {

    private lateinit var rabbitMQClient: RabbitMQClient
    private val binder = RabbitBinder()
    private lateinit var robot: Robot

    // Define CoroutineScope for the service
    private val serviceScope = CoroutineScope(Dispatchers.Main)

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
            "store_update_queue" to { message: String -> handleRabbitMqMessage(message) }
        )

        rabbitMQClient = RabbitMQClient(
            host = Config.rabbitMQHost,
            port = Config.rabbitMQPort,
            username = Config.rabbitMQUsername,
            password = Config.rabbitMQPassword,
            queues = queueHandlers
        )

        rabbitMQClient.connect() // Connect to RabbitMQ
    }

    override fun onDestroy() {
        super.onDestroy()
        rabbitMQClient.disconnect() // Disconnect when the service is destroyed

        // Cancel any ongoing coroutines when the service is destroyed
        serviceScope.cancel()
    }

    private fun handleRabbitMqMessageController(message: String) {
        Log.d("RabbitMQService", "Received message: $message")
        val robot = Robot.getInstance()
        val controller = RobotController(robot)
        controller.handleRabbitMqMessage(message)
    }

    private fun handleRabbitMqMessage(message: String) {
        Log.d("RabbitMQService", "Received message: $message")

        if (message == "UPDATE") {
            Log.d("RabbitMQService", "Received UPDATE command, fetching new product data")
            fetchDataAndUpdateUI()  // Call fetch function
        }
    }

    // Fetch data and update the UI by broadcasting
    private fun fetchDataAndUpdateUI() {
        val productRepository = ProductRepository()

        // Use the service's CoroutineScope
        serviceScope.launch {
            // Fetch product data using the repository's asynchronous method
            productRepository.getProductsData { products ->
                if (products != null) {
                    // Broadcast the product update, let the UI component handle it
                    sendBroadcast(Intent("PRODUCT_UPDATED"))

                    Log.d("RabbitMQService", "Product data fetched and UI update triggered")
                } else {
                    Log.e("RabbitMQService", "Failed to fetch product data")
                }
            }
        }
    }

    // Method to stop RabbitMQ connection manually
    fun stopRabbitMQ() {
        rabbitMQClient.disconnect()
    }
}
