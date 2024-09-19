package com.example.temiapp.ui

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.R

class LoadingActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the action bar for AppCompatActivity
        supportActionBar?.hide() // No need for `requestWindowFeature(Window.FEATURE_NO_TITLE);` in AppCompatActivity

        // Enable full-screen mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        setContentView(R.layout.activity_loading)

        hideSystemBars()

        Log.d("LoadingActivity", "Loading screen displayed.")

        // Optionally, dismiss the loading screen after a set time if needed
        handler.postDelayed({ finishLoadingScreen() }, 2000) // You can adjust/remove the delay as needed
    }

    // Call this method when the data is successfully loaded to close the loading screen
    fun finishLoadingScreen() {
        Log.d("LoadingActivity", "Loading screen finished.")
        finish() // Just finish the LoadingActivity without restarting it
    }

    private fun hideSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
        Log.d("LoadingActivity", "System bars hidden.")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Remove all callbacks when activity is destroyed
        Log.d("LoadingActivity", "LoadingActivity destroyed, callbacks removed.")
    }
}
