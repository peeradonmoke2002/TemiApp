package com.example.temiapp.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.R
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnRobotReadyListener

class LoadingActivity : AppCompatActivity(), OnRobotReadyListener {

    private lateinit var robot: Robot
    private val handler = Handler(Looper.getMainLooper())
    private val finishRunnable = Runnable {
        finish() // Close LoadingActivity after 2 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_loading)

            // Optional: Add a timeout if you want to stop the loading screen after 2 seconds
            handler.postDelayed(finishRunnable, 2000) // Show for 2 seconds

            Log.d("LoadingActivity", "Loading screen displayed for 2 seconds.")
        } catch (e: Exception) {
            Log.e("LoadingActivity", "Error during onCreate: ${e.localizedMessage}")
            e.printStackTrace()
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
            Log.d("LoadingActivity", "System bars hidden.")
        } catch (e: Exception) {
            Log.e("LoadingActivity", "Error hiding system bars: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            robot = Robot.getInstance()
            hideSystemBars()

            if (::robot.isInitialized) {
                robot.hideTopBar()
                Log.d("LoadingActivity", "Robot instance initialized and system bars hidden.")
            } else {
                Log.e("LoadingActivity", "Robot instance is not initialized.")
            }
        } catch (e: Exception) {
            Log.e("LoadingActivity", "Error during onStart: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }



    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            try {
                val activityInfo = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
                robot.onStart(activityInfo)
                Log.d("LoadingActivity", "Robot is ready and started.")
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("LoadingActivity", "Error in onRobotReady: ${e.localizedMessage}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e("LoadingActivity", "Unexpected error in onRobotReady: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }

    override fun onStop() {
        try {
            super.onStop()
            handler.removeCallbacks(finishRunnable)
            robot.removeOnRobotReadyListener(this)
            Log.d("LoadingActivity", "Callbacks removed and listener cleaned up in onStop.")
        } catch (e: Exception) {
            Log.e("LoadingActivity", "Error during onStop: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }


    override fun onDestroy() {
        try {
            super.onDestroy()
            handler.removeCallbacks(finishRunnable) // Just to be safe, remove the runnable in onDestroy as well
            Log.d("LoadingActivity", "Callbacks removed in onDestroy.")
        } catch (e: Exception) {
            Log.e("LoadingActivity", "Error during onDestroy: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }
}
