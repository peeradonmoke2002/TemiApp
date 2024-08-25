package com.example.temiapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.robotemi.sdk.*
import com.robotemi.sdk.listeners.OnRobotReadyListener
import kotlinx.coroutines.*
import android.widget.VideoView

class MainActivity : AppCompatActivity(), OnRobotReadyListener {

    private lateinit var robot: Robot
    private lateinit var locationManager: LocationManager
    private lateinit var temiStatusHandler: TemiStatusHandler
    private lateinit var rabbitMqManager: RabbitMqManager

    private val rabbitMqHost = "10.62.31.238"
    private val rabbitMqPort = 5672
    private val rabbitMqUsername = "admin"
    private val rabbitMqPassword = "123456"
    private val rabbitMqQueueName = "temi_control_queue"


    private lateinit var videoView: VideoView
    private val inactivityTimeout: Long = 60000 // 1 minute
    private val handler = Handler(Looper.getMainLooper())
    private val inactivityRunnable = Runnable { playInactivityVideo() }

    private var x = 0f
    private var y = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupButtons()
        videoView = findViewById(R.id.videoView)
        videoView.setVideoPath("android.resource://" + packageName + "/" + R.raw.poomjaibot_eye_face)

        robot = Robot.getInstance()
        temiStatusHandler = TemiStatusHandler(robot)
        locationManager = LocationManager(robot, this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rabbitMqManager = RabbitMqManager(
            host = rabbitMqHost,
            port = rabbitMqPort,
            username = rabbitMqUsername,
            password = rabbitMqPassword,
            queueName = rabbitMqQueueName
        )

        rabbitMqManager.setMessageHandler(object : RabbitMqManager.MessageHandler {
            override fun onMessageReceived(message: String) {
                handleRabbitMqMessage(message)
            }
        })

        rabbitMqManager.connect()



    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnSavePosition).setOnClickListener {
            locationManager.showSavePositionDialog()
        }

        findViewById<Button>(R.id.btnGoHome).setOnClickListener {
            locationManager.goToLocation("Temi-home")
        }

        findViewById<Button>(R.id.btnGoPositionA).setOnClickListener {
            locationManager.goToLocation("Position a")
        }

        findViewById<Button>(R.id.btnGoPositionB).setOnClickListener {
            locationManager.goToLocation("Position b")
        }
    }

    override fun onStart() {
        super.onStart()
        robot.addOnRobotReadyListener(this)
    }

    override fun onStop() {
        super.onStop()
        robot.removeOnRobotReadyListener(this)
        temiStatusHandler.cleanUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        rabbitMqManager.disconnect()
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
                "W_UP", "S_UP" -> x = 0f
                "A_UP", "D_UP" -> y = 0f
            }
            robot.skidJoy(x, y, true)
            handler.postDelayed({}, 500)
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetInactivityTimer() // Reset the timer on any user interaction
    }

    private fun resetInactivityTimer() {
        handler.removeCallbacks(inactivityRunnable)
        handler.postDelayed(inactivityRunnable, inactivityTimeout)
    }

    private fun playInactivityVideo() {
        videoView.visibility = View.VISIBLE
        videoView.start()
        videoView.setOnCompletionListener { videoView.start() } // Loop the video
    }

    private fun stopInactivityVideo() {
        if (videoView.isPlaying) {
            videoView.stopPlayback()
            videoView.visibility = View.GONE
        }
    }
}
