package com.example.temiapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.MainActivity
import com.example.temiapp.databinding.ActivityErrorBinding
import com.example.temiapp.utils.Utils

class ErrorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityErrorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the action bar and system UI for full-screen experience
        supportActionBar?.hide()
        Utils.hideSystemBars(window)

        // Inflate the layout using ViewBinding
        binding = ActivityErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set retry button click listener to reload MainActivity
        binding.retryButton.setOnClickListener {
            retryMainActivity()
        }

        Log.d("ErrorActivity", "ErrorActivity successfully created.")
    }

    private fun retryMainActivity() {
        try {
            // Custom transition animations for smooth effect
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish() // Close the ErrorActivity
        } catch (e: Exception) {
            // Log error and show a message to the user (optional)
            Log.e("ErrorActivity", "Error starting MainActivity: ${e.localizedMessage}")
        }
    }
}
