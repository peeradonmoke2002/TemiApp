package com.example.temiapp.ui

import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.R
import com.example.temiapp.network.RetrofitClient
import com.example.temiapp.data.ProductApi
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QRCodeActivity : AppCompatActivity() {

    private lateinit var qrCodeImageView: ImageView

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
        setContentView(R.layout.activity_qr_code)

        qrCodeImageView = findViewById(R.id.qrCodeImageView)

        // Fetch product ID from intent
        val productId = intent.getIntExtra("PRODUCT_ID", -1)

        if (productId != -1) {
            fetchQrCode(productId)
        } else {
            Log.e("QRCodeActivity", "Invalid product ID: $productId")
        }
    }

    // Correct method to handle button click for closing the activity
    fun onCloseButtonClick(view: View) {
        try {
            Log.d("QRCodeActivity", "Closing QRCodeActivity")
            setResult(RESULT_OK)  // Set result for returning to the previous activity
            finish() // Close the activity
        } catch (e: Exception) {
            Log.e("QRCodeActivity", "Error during onCloseButtonClick: ${e.localizedMessage}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("QRCodeActivity", "Activity Destroyed")
    }

    private fun fetchQrCode(productId: Int) {
        val productApi = RetrofitClient.retrofit.create(ProductApi::class.java)
        productApi.getQrCodeImage(productId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val inputStream = responseBody.byteStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        qrCodeImageView.setImageBitmap(bitmap)
                    }
                } else {
                    Log.e("QRCodeActivity", "Failed to load QR code: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("QRCodeActivity", "API call failed", t)
            }
        })
    }
}
