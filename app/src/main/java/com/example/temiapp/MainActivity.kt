package com.example.temiapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.temiapp.data.Product
import com.example.temiapp.data.ProductApi
import com.example.temiapp.network.RetrofitClient
import com.example.temiapp.ui.ProductPageAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.example.temiapp.ui.ErrorActivity
import com.example.temiapp.ui.LoadingActivity
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.example.temiapp.network.RabbitMQClient
import com.example.temiapp.controller.RobotController
import com.example.temiapp.network.RabbitMQService

class MainActivity : AppCompatActivity(), OnRobotReadyListener  {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductPageAdapter
    private lateinit var rabbitMQClient: RabbitMQClient
    private lateinit var controller: RobotController
    private lateinit var robot: Robot
    private var rabbitMQService: RabbitMQService? = null
    private var isBound = false

    // Handler and Runnable for inactivity detection
    private val handler = Handler(Looper.getMainLooper())
    private val inactivityTimeout: Long = 30000 // 30 seconds of inactivity timeout

    private val inactivityRunnable = Runnable {
        // After 30 seconds of inactivity, switch back to VideoActivity
        val intent = Intent(this@MainActivity, com.example.temiapp.ui.VideoActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity
    }


    // Create a connection to the RabbitMQService
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RabbitMQService.RabbitBinder
            var rabbitMQService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            rabbitMQService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        robot = Robot.getInstance()

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        productAdapter = ProductPageAdapter(this, listOf(), hasError = false, dataIsEmpty = false)
        recyclerView.adapter = productAdapter

        hideSystemBars()

        // Attach PagerSnapHelper for page-like swiping (3 products per page)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // Fetch and handle product data
        fetchAndHandleProductsData()
        // Bind to RabbitMQService
        val intent = Intent(this, RabbitMQService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

        // Start the inactivity handler
        handler.postDelayed(inactivityRunnable, inactivityTimeout)

    }

    private fun fetchProductsData(onResult: (Boolean) -> Unit) {
        val productApi = RetrofitClient.retrofit.create(ProductApi::class.java)

        productApi.getProductsData().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful) {
                    val products = response.body() ?: listOf()

                    // Split the product list into pages (each page with up to 3 products)
                    val productPages = products.chunked(3)

                    // Update the adapter with the actual paginated data
                    productAdapter.updateData(productPages)

                    // Notify success
                    onResult(true)
                } else {
                    Log.e("MainActivity", "Failed to fetch products")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                Log.e("MainActivity", "API call failed", t)
                onResult(false)
            }
        })
    }

    private fun startLoadingScreen() {
        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)
    }

    private fun showErrorScreen() {
        val intent = Intent(this, ErrorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Close MainActivity
    }

    private fun fetchAndHandleProductsData() {
        // Start the loading screen
        startLoadingScreen()

        fetchProductsData { isSuccess ->
            // If success, stop the loading screen and proceed with normal flow
            if (isSuccess) {
                // Close the LoadingActivity after success (assuming LoadingActivity is on top)
                Handler(Looper.getMainLooper()).postDelayed({
                    // Finish LoadingActivity (you can send a broadcast or finish it explicitly if necessary)
                    sendBroadcast(Intent(LoadingActivity::class.java.name).apply {
                        action = "finish_loading"
                    })
                }, 5000) // Optional delay for UX reasons
            } else {
                // If error, show error screen and finish MainActivity
                showErrorScreen()
            }
        }
    }


    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android R (API level 30) and above
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For older versions, use system UI flags
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }

    // Reset inactivity timer on any user interaction
    override fun onUserInteraction() {
        super.onUserInteraction()
        handler.removeCallbacks(inactivityRunnable) // Remove current callback
        handler.postDelayed(inactivityRunnable, inactivityTimeout) // Reset the timer
    }

    override fun onStart() {
        super.onStart()
        robot.addOnRobotReadyListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind from the service to avoid leaks
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        handler.removeCallbacks(inactivityRunnable) // Stop the handler when activity is destroyed
    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            try {
                val activityInfo = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
                robot.onStart(activityInfo)
                robot.speak(TtsRequest.create("Temi is ready", false))
            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException(e)
            }
        }
    }
}
