package com.example.temiapp.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.MainActivity
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
        setContentView(R.layout.activity_qr_code)

        qrCodeImageView = findViewById(R.id.qrCodeImageView)

        // Assume we are passing the product ID to this activity via intent
        val productId = intent.getIntExtra("PRODUCT_ID", -1)

        if (productId != -1) {
            fetchQrCode(productId)
        }
    }

    // Correct method to handle button click for closing the activity
    fun onCloseButtonClick(view: View) {
        finish()
    }

    private fun fetchQrCode(productId: Int) {
        val productApi = RetrofitClient.retrofit.create(ProductApi::class.java)
        productApi.getQrCodeImage(productId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        // Convert the response body to a Bitmap
                        val inputStream = responseBody.byteStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        // Display the bitmap in the ImageView
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
