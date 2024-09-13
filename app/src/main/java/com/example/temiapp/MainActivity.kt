package com.example.temiapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.temiapp.data.Product
import com.example.temiapp.data.ProductApi
import com.example.temiapp.network.RetrofitClient
import com.example.temiapp.ui.ProductPageAdapter
import com.example.temiapp.network.WebRTCManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.app.Activity
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.content.Context



class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductPageAdapter
    private lateinit var webRTCManager: WebRTCManager
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private val SCREEN_CAPTURE_REQUEST_CODE = 1000
    // Handler and Runnable for inactivity detection
    private val handler = Handler()
    private val inactivityTimeout: Long = 30000 // 30 seconds of inactivity timeout

    private val inactivityRunnable = object : Runnable {
        override fun run() {
            // After 30 seconds of inactivity, switch back to VideoActivity
            val intent = Intent(this@MainActivity, com.example.temiapp.ui.VideoActivity::class.java)
            startActivity(intent)
            finish() // Close MainActivity
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        productAdapter = ProductPageAdapter(this, listOf(), hasError = false, dataIsEmpty = false)
        recyclerView.adapter = productAdapter

        // Attach PagerSnapHelper for page-like swiping (3 products per page)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // Fetch products and update the adapter when data is available
        fetchProductsData()

        // Initialize WebRTCManager with signaling server URL
        webRTCManager = WebRTCManager(this, "ws://192.168.1.113:8080")
        webRTCManager.init()

        // Start the call when appropriate (e.g., on a button click or automatically)
        webRTCManager.startCall()

        // Start the inactivity handler
        handler.postDelayed(inactivityRunnable, inactivityTimeout)
    }

    private fun fetchProductsData() {
        val productApi = RetrofitClient.retrofit.create(ProductApi::class.java)

        productApi.getProductsData().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful) {
                    val products = response.body() ?: listOf()

                    // Split the product list into pages (each page with up to 3 products)
                    val productPages = products.chunked(3)

                    // Update the adapter with the actual paginated data
                    productAdapter.updateData(productPages)
                } else {
                    Log.e("MainActivity", "Failed to fetch products")
                }
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                Log.e("MainActivity", "API call failed", t)
            }
        })
    }

    // Request screen capture permission
    private fun startScreenCapture() {
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, SCREEN_CAPTURE_REQUEST_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Pass the Intent (data) to WebRTCManager to start screen sharing
                webRTCManager.startScreenCapture(data)
            } else {
                Log.e("MainActivity", "Screen capture permission denied.")
            }
        }
    }




    // Reset inactivity timer on any user interaction
    override fun onUserInteraction() {
        super.onUserInteraction()
        handler.removeCallbacks(inactivityRunnable) // Remove current callback
        handler.postDelayed(inactivityRunnable, inactivityTimeout) // Reset the timer
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(inactivityRunnable) // Stop the handler when activity is destroyed
    }
}
