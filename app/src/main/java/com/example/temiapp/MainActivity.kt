package com.example.temiapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.temiapp.data.Product
import com.example.temiapp.data.ProductApi
import com.example.temiapp.network.RetrofitClient
import com.example.temiapp.network.RTSPStreamingService
import com.example.temiapp.ui.ErrorActivity
import com.example.temiapp.ui.LoadingActivity
import com.example.temiapp.ui.ProductPageAdapter
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnRobotReadyListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.temiapp.network.RabbitMQService

class MainActivity : AppCompatActivity(), OnRobotReadyListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductPageAdapter
    private lateinit var robot: Robot

    private var rabbitMQService: RabbitMQService? = null
    private var rtspStreamingService: RTSPStreamingService? = null
    private var isBoundRabbit = false
    private var isBoundRTSP = false

    // Handler and Runnable for inactivity detection
    private val handler = Handler(Looper.getMainLooper())
    private val inactivityTimeout: Long = 30000 // 30 seconds of inactivity timeout

    private val inactivityRunnable = Runnable {
        val intent = Intent(this@MainActivity, com.example.temiapp.ui.VideoActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity after inactivity
    }

    // Service connections
    private val rabbitMQServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RabbitMQService.RabbitBinder
            rabbitMQService = binder.getService()
            isBoundRabbit = true
            Log.d("MainActivity", "RabbitMQService bound")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            rabbitMQService = null
            isBoundRabbit = false
            Log.d("MainActivity", "RabbitMQService unbound")
        }
    }

    private val rtspServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RTSPStreamingService.RSTPBinder
            rtspStreamingService = binder.getService()
            isBoundRTSP = true
            Log.d("MainActivity", "RTSPStreamingService bound")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            rtspStreamingService = null
            isBoundRTSP = false
            Log.d("MainActivity", "RTSPStreamingService unbound")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        robot = Robot.getInstance()

        // Initialize RecyclerView
        setupRecyclerView()

        // Hide system bars
        hideSystemBars()

        // Fetch and handle product data
        fetchAndHandleProductsData()

        // Start the inactivity handler
        handler.postDelayed(inactivityRunnable, inactivityTimeout)

        // Bind to RabbitMQService
        bindService(Intent(this, RabbitMQService::class.java), rabbitMQServiceConnection, BIND_AUTO_CREATE)

        // Bind to RTSPStreamingService
        bindService(Intent(this, RTSPStreamingService::class.java), rtspServiceConnection, BIND_AUTO_CREATE)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        productAdapter = ProductPageAdapter(this, listOf(), hasError = false, dataIsEmpty = false)
        recyclerView.adapter = productAdapter

        // Attach PagerSnapHelper for smooth swiping
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
    }

    private fun fetchProductsData(onResult: (Boolean) -> Unit) {
        val productApi = RetrofitClient.retrofit.create(ProductApi::class.java)
        productApi.getProductsData().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful) {
                    val products = response.body() ?: listOf()
                    val productPages = products.chunked(3) // Split into pages with up to 3 products
                    productAdapter.updateData(productPages)
                    onResult(true)
                } else {
                    Log.e("MainActivity", "Failed to fetch products")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                Log.e("MainActivity", "API call failed: ${t.localizedMessage}")
                // You could show an error message to the user here
                onResult(false)
            }

        })
    }

    private fun fetchAndHandleProductsData() {
        startLoadingScreen()

        fetchProductsData { isSuccess ->
            if (isSuccess) {
                // Stop the loading screen
                Handler(Looper.getMainLooper()).postDelayed({
                    sendBroadcast(Intent(LoadingActivity::class.java.name).apply {
                        action = "finish_loading"
                    })
                }, 5000) // Optional delay for smoother UX
            } else {
                showErrorScreen()
            }
        }
    }

    private fun startLoadingScreen() {
        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)
    }

    private fun showErrorScreen() {
        val intent = Intent(this, ErrorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Close MainActivity on error
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
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

    // Reset inactivity timer on user interaction
    override fun onUserInteraction() {
        super.onUserInteraction()
        handler.removeCallbacks(inactivityRunnable)
        handler.postDelayed(inactivityRunnable, inactivityTimeout)
    }

    override fun onStart() {
        super.onStart()
        robot.addOnRobotReadyListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unbind services to avoid memory leaks
        if (isBoundRabbit) {
            unbindService(rabbitMQServiceConnection)
            isBoundRabbit = false
        }
        if (isBoundRTSP) {
            unbindService(rtspServiceConnection)
            isBoundRTSP = false
        }

        handler.removeCallbacks(inactivityRunnable) // Remove the inactivity handler
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
