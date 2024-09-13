package com.example.temiapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.R

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // Optional: Add a timeout if you want to stop the loading screen after a certain time
        Handler(Looper.getMainLooper()).postDelayed({
            // Close LoadingActivity after timeout if the API call takes too long
            finish()
        }, 10000) // 30 seconds timeout
    }
}
