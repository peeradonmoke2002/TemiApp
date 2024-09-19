package com.example.temiapp.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.MainActivity
import com.example.temiapp.databinding.ActivityErrorBinding
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnRobotReadyListener

class ErrorActivity : AppCompatActivity(), OnRobotReadyListener {

    // Declare the binding object
    private lateinit var binding: ActivityErrorBinding
    private lateinit var robot: Robot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize the robot instance
            robot = Robot.getInstance()

            // Hide top bar immediately after the robot is initialized
            robot.hideTopBar()
            hideSystemBars()

            // Initialize the View Binding
            binding = ActivityErrorBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Set click listener on retry button
            binding.retryButton.setOnClickListener {
                try {
                    // Restart MainActivity to try reconnecting to the API
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish() // Close the current ErrorActivity
                } catch (e: Exception) {
                    Log.e("ErrorActivity", "Error starting MainActivity: ${e.localizedMessage}")
                    e.printStackTrace()
                }
            }

            Log.d("ErrorActivity", "ErrorActivity successfully created.")
        } catch (e: Exception) {
            Log.e("ErrorActivity", "Error during onCreate: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            try {
                if (::robot.isInitialized) {
                    val activityInfo = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
                    robot.onStart(activityInfo)
                    Log.d("ErrorActivity", "Robot is ready and started.")
                } else {
                    Log.e("ErrorActivity", "Robot instance is not initialized.")
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("ErrorActivity", "Error in onRobotReady: ${e.localizedMessage}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e("ErrorActivity", "Unexpected error in onRobotReady: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }

    private fun hideSystemBars() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.apply {
                    hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
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
            Log.d("ErrorActivity", "System bars hidden successfully.")
        } catch (e: Exception) {
            Log.e("ErrorActivity", "Error hiding system bars: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        try {
            robot.removeOnRobotReadyListener(this) // Clean up robot listener
            super.onDestroy()
            Log.d("ErrorActivity", "Resources cleaned up in onDestroy.")
        } catch (e: Exception) {
            Log.e("ErrorActivity", "Error during onDestroy: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }
}
