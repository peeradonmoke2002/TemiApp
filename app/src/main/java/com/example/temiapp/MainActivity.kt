package com.example.temiapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnRobotReadyListener
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), OnRobotReadyListener {

    private lateinit var robot: Robot
    private lateinit var executor: ExecutorService

    private lateinit var connectionFactory: ConnectionFactory
    private lateinit var connection: Connection
    private lateinit var channel: Channel

    // RabbitMQ configuration
    private val rabbitMqHost = "10.62.31.238"
    private val rabbitMqPort = 5672
    private val rabbitMqUsername = "admin"
    private val rabbitMqPassword = "123456"
    private val rabbitMqQueueName = "temi_control_queue"

    private var x = 0f
    private var y = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        robot = Robot.getInstance()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRabbitMq()
    }


    private fun setupRabbitMq() {
        executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                // Add this log statement to help debug the connection issue
                Log.d("MainActivity", "Attempting to connect to RabbitMQ at $rabbitMqHost:$rabbitMqPort with user $rabbitMqUsername")

                connectionFactory = ConnectionFactory().apply {
                    host = rabbitMqHost
                    port = rabbitMqPort
                    username = rabbitMqUsername
                    password = rabbitMqPassword
                }

                connection = connectionFactory.newConnection()
                if (connection.isOpen) {
                    Log.d("MainActivity", "Connection established successfully.")
                } else {
                    Log.e("MainActivity", "Connection failed to open.")
                    return@execute
                }

                channel = connection.createChannel()

                channel.queueDeclare(rabbitMqQueueName, false, false, false, mapOf("x-message-ttl" to 300000))


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

                channel.basicConsume(rabbitMqQueueName, true, consumer)
                Log.d("MainActivity", "Connected and consuming messages from $rabbitMqQueueName")

            } catch (e: Exception) {
                Log.e("MainActivity", "Error connecting to RabbitMQ: ${e.message}", e)
            }
        }
    }


    private fun handleRabbitMqMessage(message: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            when (message) {
                "X", "Z", "C" -> {
                    robot.stopMovement()
                    return@post
                }
                "W_DOWN" -> x = if (y != 0f) 0.5f else 0.6f
                "S_DOWN" -> x = if (y != 0f) -0.4f else -0.6f
                "A_DOWN" -> y = if (x < 0) -1f else 1f
                "D_DOWN" -> y = if (x < 0) 1f else -1f
                "W_UP", "S_UP" -> x = 0f  // Stop forward/backward movement
                "A_UP", "D_UP" -> y = 0f  // Stop left/right movement
            }
            robot.skidJoy(x, y, false)
        }
    }



    override fun onStart() {
        super.onStart()
        robot.addOnRobotReadyListener(this)
    }

    override fun onStop() {
        super.onStop()
        robot.removeOnRobotReadyListener(this)
        executor.shutdownNow()
        disconnectRabbitMq()
    }

    private fun disconnectRabbitMq() {
        try {
            channel.close()
            connection.close()
            Log.d("MainActivity", "Disconnected from RabbitMQ")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error disconnecting from RabbitMQ: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectRabbitMq()
    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            try {
                val activityInfo = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
                robot.onStart(activityInfo)
                robot.speak(TtsRequest.create("Temi is ready", false))
            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException(e)
            }
        }
    }
}
