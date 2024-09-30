package com.example.temiapp.ui

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.temiapp.R
import com.example.temiapp.data.ProductRepository
import com.example.temiapp.utils.Utils

class QRCodeActivity : AppCompatActivity() {

    private lateinit var qrCodeImageView: ImageView
    private lateinit var detail: TextView
    private lateinit var productRepository: ProductRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the action bar for AppCompatActivity
        supportActionBar?.hide()
        Utils.hideSystemBars(window)

        setContentView(R.layout.activity_qr_code)

        // Initialize views
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        detail = findViewById(R.id.detail)

        // Initialize product repository
        productRepository = ProductRepository()

        // Fetch product ID from intent
        val productId = intent.getIntExtra("PRODUCT_ID", -1)

        if (productId != -1) {
            fetchQrCodeImage(productId)  // Fetch QR code image
            fetchProductDetails(productId)  // Fetch product details
        } else {
            Log.e("QRCodeActivity", "Invalid product ID: $productId")
        }
    }

    // Correct method to handle button click for closing the activity
    fun onCloseButtonClick(view: View) {
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
                var inputStream = responseBody.byteStream()
                try {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    qrCodeImageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e("QRCodeActivity", "Error decoding QR code image: ${e.localizedMessage}")
                    qrCodeImageView.setImageResource(R.drawable.placeholder)  // Set a placeholder image in case of error
                } finally {
                    // Ensure inputStream is always closed
                    inputStream.close()
                }
            } else {
                Log.e("QRCodeActivity", "Failed to fetch QR code for product ID: $productId")
                qrCodeImageView.setImageResource(R.drawable.placeholder)  // Set placeholder if fetching fails
            }
        }
    }

    // Fetch and display product details
    @SuppressLint("SetTextI18n")
    private fun fetchProductDetails(productId: Int) {
        productRepository.getProductDetails(productId) { product ->
            if (product != null) {
                try {
                    // Assuming `detail` is a TextView to display product details
                    val productDetails = product.detail
                    detail.text = productDetails
                } catch (e: Exception) {
                    Log.e("QRCodeActivity", "Error displaying product details: ${e.localizedMessage}")
                    detail.text = "No data found"
                }
            } else {
                Log.e("QRCodeActivity", "Product details not found for product ID: $productId")
                detail.text = "Error fetching details"
            }
        }
    }
}
