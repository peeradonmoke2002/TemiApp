package com.example.temiapp.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MotionEvent
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.MainActivity
import com.example.temiapp.R
import com.example.temiapp.network.RabbitMQService
import com.example.temiapp.utils.Utils
import com.robotemi.sdk.Robot

class VideoActivity : AppCompatActivity() {

    private lateinit var robot: Robot
    private lateinit var temiface: VideoView
    private var rabbitMQService: RabbitMQService? = null
    private var isBound = false

    private var isVideoPrepared = false
    private var isRobotInitialized = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RabbitMQService.RabbitBinder
            rabbitMQService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            rabbitMQService = null
            isBound = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        Utils.hideSystemBars(window)
        setContentView(R.layout.activity_video)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setupVideoView()

        val intent = Intent(this, RabbitMQService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

        temiface.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                changeToMainMenu()
            }
            false
        }
    }



    private fun changeToMainMenu() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        robot = Robot.getInstance()
        robot.hideTopBar()

        isRobotInitialized = true
        maybeTiltRobot()  // Try tilting robot if video is also ready

        Log.e("VideoActivity", "Activity Started")
    }

    private fun setupVideoView() {
        temiface = findViewById(R.id.videoView)
        val uri = Uri.parse("android.resource://$packageName/${R.raw.poomjaibot_eye_face}")
        temiface.setVideoURI(uri)

        temiface.setOnPreparedListener { mp ->
            temiface.start()
            mp.isLooping = true

            isVideoPrepared = true
            maybeTiltRobot()  // Try tilting robot if robot is also initialized
        }
    }


    private fun maybeTiltRobot() {
        if (isRobotInitialized && isVideoPrepared) {
            robot.tiltAngle(0, 1f)
        }
    }

    override fun onPause() {
        super.onPause()
        temiface.pause()
    }

    override fun onResume() {
        super.onResume()
        temiface.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }

        temiface.stopPlayback()
        Log.e("VideoActivity", "Activity Destroyed")
    }
}
