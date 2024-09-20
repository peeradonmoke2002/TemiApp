package com.example.temiapp.ui

import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.R
import com.example.temiapp.data.ProductRepository


class QRCodeActivity : AppCompatActivity() {

    private lateinit var qrCodeImageView: ImageView
    private lateinit var productRepository: ProductRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the action bar for AppCompatActivity
        supportActionBar?.hide()

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

        setContentView(R.layout.activity_qr_code)

        qrCodeImageView = findViewById(R.id.qrCodeImageView)

        // Initialize product repository
        productRepository = ProductRepository()

        // Fetch product ID from intent
        val productId = intent.getIntExtra("PRODUCT_ID", -1)

        if (productId != -1) {
            fetchQrCodeImage(productId)  // Fetch QR code image
        } else {
            Log.e("QRCodeActivity", "Invalid product ID: $productId")
        }
    }

    // Correct method to handle button click for closing the activity
    fun onCloseButtonClick() {
        try {
            Log.d("QRCodeActivity", "Closing QRCodeActivity")
            setResult(RESULT_OK)  // Set result for returning to the previous activity
            finish()  // Close the activity
        } catch (e: Exception) {
            Log.e("QRCodeActivity", "Error during onCloseButtonClick: ${e.localizedMessage}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("QRCodeActivity", "Activity Destroyed")
    }

    // Fetch the QR code image and display it in the ImageView
    private fun fetchQrCodeImage(productId: Int) {
        productRepository.getQrCodeImage(productId) { responseBody ->
            if (responseBody != null) {
                try {
                    val inputStream = responseBody.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    qrCodeImageView.setImageBitmap(bitmap)
                    inputStream.close()  // Close the input stream to prevent leaks
                } catch (e: Exception) {
                    Log.e("QRCodeActivity", "Error decoding QR code image: ${e.localizedMessage}")
                    qrCodeImageView.setImageResource(R.drawable.placeholder)  // Set a placeholder image in case of error
                }
            } else {
                Log.e("QRCodeActivity", "Failed to fetch QR code for product ID: $productId")
                qrCodeImageView.setImageResource(R.drawable.placeholder)  // Set placeholder if fetching fails
            }
        }
    }
}
