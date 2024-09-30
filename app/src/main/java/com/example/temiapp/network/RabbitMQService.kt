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
import com.example.temiapp.MainActivity

class RabbitMQService : Service() {

    private lateinit var rabbitMQConnection: RabbitMQConnection
    private lateinit var rabbitMQClient: RabbitMQClient
    private lateinit var rabbitMQSender: RabbitMQSender
    private val binder = RabbitBinder()
    private lateinit var robot: Robot
    private var mainActivity: MainActivity? = null
    private var robotController: RobotController? = null

    // Use Dispatchers.IO for I/O operations
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    inner class RabbitBinder : Binder() {
        fun getService(): RabbitMQService = this@RabbitMQService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        robot = Robot.getInstance()

        // Initialize RabbitMQConnection
        rabbitMQConnection = RabbitMQConnection(
            host = Config.rabbitMQHost,
            port = Config.rabbitMQPort,
            username = Config.rabbitMQUsername,
            password = Config.rabbitMQPassword
        )

        // Initialize RabbitMQSender
        rabbitMQSender = RabbitMQSender(rabbitMQConnection)

        val queueHandlers = mapOf(
            "robot_control_queue" to { message: String -> handleRabbitMqMessageController(message) },
            "store_update_queue" to { message: String -> handleRabbitMqMessageStore(message) },
            "robot_control_head_queue" to { message: String -> handleRabbitMqHeadMessageController(message) }
        )

        // Initialize RabbitMQClient using RabbitMQConnection
        rabbitMQClient = RabbitMQClient(
            connection = rabbitMQConnection,
            queues = queueHandlers
        )

        rabbitMQClient.setConnectionListener(object : RabbitMQClient.ConnectionListener {
            override fun onConnected() {
                Log.d("RabbitMQService", "RabbitMQ connected")
            }

            override fun onDisconnected() {
                Log.e("RabbitMQService", "RabbitMQ disconnected, retrying...")
                reconnect()
            }
        })

        // Start consuming messages in a background thread
        serviceScope.launch {
            rabbitMQClient.startConsuming()
        }
    }

    private fun reconnect() {
        // Retry logic to reconnect to RabbitMQ
        serviceScope.launch {
            rabbitMQClient.stopConsuming()
            rabbitMQClient.startConsuming()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rabbitMQClient.stopConsuming()
        serviceScope.cancel()
    }

    fun setMainActivity(activity: MainActivity) {
        this.mainActivity = activity
        robotController = RobotController(robot, mainActivity!!)
    }

    private fun handleRabbitMqMessageController(message: String) {
        Log.d("RabbitMQService", "Received message: $message")

        robotController?.handleRabbitMqControllMessage(message)
            ?: Log.e("RabbitMQService", "RobotController is not initialized")
    }

    private fun handleRabbitMqMessageStore(message: String) {
        Log.d("RabbitMQService", "Received message: $message")

        if (message == "UPDATE") {
            Log.d("RabbitMQService", "Received UPDATE command, fetching new product data")
            fetchDataAndUpdateUI()
        } else {
            Log.d("RabbitMQService", "Received message: $message")
        }
    }

    private fun handleRabbitMqHeadMessageController(message: String) {
        Log.d("RabbitMQService", "Received message: $message")
        robotController?.handleRabbitMqHeadControllMessage(message)
            ?: Log.e("RabbitMQService", "RobotController is not initialized")
    }

    private fun fetchDataAndUpdateUI() {
        val productRepository = ProductRepository()

        serviceScope.launch {
            productRepository.getProductsData { products ->
                if (products != null) {
                    // Broadcast an intent indicating the product data was updated
                    sendBroadcast(Intent("PRODUCT_UPDATED"))
                    Log.d("RabbitMQService", "Product data fetched and UI update triggered")
                } else {
                    Log.e("RabbitMQService", "Failed to fetch product data")
                }
            }
        }
    }

    // Expose sendMessage method to other classes (like MainActivity)
    fun sendMessage(queueName: String, message: String) {
        serviceScope.launch {
            try {
                rabbitMQSender.sendMessage(queueName, message)
                Log.d("RabbitMQService", "Message sent to $queueName: $message")
            } catch (e: Exception) {
                Log.e("RabbitMQService", "Error sending message: ${e.message}", e)
            }
        }
    }

    fun stopRabbitMQ() {
        rabbitMQClient.stopConsuming()
    }
}
