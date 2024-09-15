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

        // Optional: Add a timeout if you want to stop the loading screen after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            finish() // Close LoadingActivity after 5 seconds
        }, 2000) // Show for 5 seconds
    }
}
