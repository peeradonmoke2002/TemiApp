package com.example.temiapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.MainActivity
import com.example.temiapp.databinding.ActivityErrorBinding

class ErrorActivity : AppCompatActivity() {

    // Declare the binding object
    private lateinit var binding: ActivityErrorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the View Binding
        binding = ActivityErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set click listener on retry button
        binding.retryButton.setOnClickListener {
            // Restart MainActivity to try reconnecting to the API
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
