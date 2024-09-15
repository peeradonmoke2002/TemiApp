package com.example.temiapp.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.MainActivity
import com.example.temiapp.R
import com.example.temiapp.network.RabbitMQService

class VideoActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private var rabbitMQService: RabbitMQService? = null
    private var isBound = false

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        // Bind to RabbitMQService
        val intent = Intent(this, RabbitMQService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

        videoView = findViewById(R.id.videoView)

        // Enable fullscreen immersive mode
        hideSystemBars()

        // Set the video URI (replace with your video file or URL)
        val videoUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.poomjaibot_eye_face)
        videoView.setVideoURI(videoUri)

        // Start playing the video
        videoView.start()

        // Loop the video when it finishes
        videoView.setOnCompletionListener {
            videoView.start() // Loop the video
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android R (API level 30) and above
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For older versions, use system UI flags
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }

    // Detect user touch and switch to MainActivity
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            // Switch to MainActivity when the screen is touched
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close VideoActivity
        }
        return super.onTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars() // Ensure system bars are hidden when returning to activity
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unbind from the service
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}
