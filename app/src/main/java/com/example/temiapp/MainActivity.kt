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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.temiapp.data.ProductApi
import com.example.temiapp.network.RabbitMQService
import com.example.temiapp.network.RetrofitClient
import com.example.temiapp.ui.ErrorActivity
import com.example.temiapp.ui.LoadingActivity
import com.example.temiapp.ui.ProductPageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnRobotReadyListener


class MainActivity : AppCompatActivity(), OnRobotReadyListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductPageAdapter
    private var rabbitMQService: RabbitMQService? = null
    private var isBoundRabbit = false
    private val inactivityTimeout: Long = 30000 // 30 seconds timeout
    private val handler = Handler(Looper.getMainLooper())
    private val inactivityRunnable = Runnable { changeTemiFace() }
    private var isLoadingScreenActive = false

    private lateinit var robot: Robot


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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the action bar
        supportActionBar?.hide()
        hideSystemBars()
        setContentView(R.layout.activity_main)

        handler.postDelayed(inactivityRunnable, inactivityTimeout)

        // Initialize Temi Robot
        robot = Robot.getInstance()


        // Bind to RabbitMQService
        bindService(Intent(this, RabbitMQService::class.java), rabbitMQServiceConnection, BIND_AUTO_CREATE)


        // Setup RecyclerView
        setupRecyclerView()

        // Fetch and handle product data
        fetchAndHandleProductsData()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars() // Ensure the system bars are hidden
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        productAdapter = ProductPageAdapter(this, listOf(), hasError = false, dataIsEmpty = false)
        recyclerView.adapter = productAdapter

        if (recyclerView.onFlingListener == null) {
            PagerSnapHelper().attachToRecyclerView(recyclerView)
        }
    }



    private fun fetchProductsData(onResult: (Boolean) -> Unit) {
        val productApi = RetrofitClient.retrofit.create(ProductApi::class.java)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    productApi.getProductsData().execute()
                }

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val products = response.body()!!
                    productAdapter.updateData(products.chunked(3))
                    onResult(true)
                } else {
                    Log.e("MainActivity", "Failed to fetch products, response: ${response.message()}")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching products: ${e.localizedMessage}")
                onResult(false)
            }
        }
    }

    private fun fetchAndHandleProductsData() {
        Log.d("MainActivity", "Starting product data fetch")
        startLoadingScreen()

        fetchProductsData { isSuccess ->
            runOnUiThread {
                closeLoadingScreen()

                if (isSuccess) {
                    Log.d("MainActivity", "Product data fetched successfully")
                } else {
                    Log.e("MainActivity", "Product data fetch failed, showing error screen")
                    showErrorScreen()
                }
            }
        }
    }

    private fun startLoadingScreen() {
        if (!isLoadingScreenActive) {
            isLoadingScreenActive = true
            Log.d("MainActivity", "Showing loading screen")
            val intent = Intent(this, LoadingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun closeLoadingScreen() {
        if (isLoadingScreenActive) {
            isLoadingScreenActive = false
            Log.d("MainActivity", "Closing loading screen")
            val finishIntent = Intent("finish_loading_screen")
            sendBroadcast(finishIntent)
        }
    }

    private fun showErrorScreen() {
        Log.d("MainActivity", "Launching error screen")
        val intent = Intent(this, ErrorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
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
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
        Log.d("MainActivity", "System bars hidden.")
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        Log.d("MainActivity", "User interaction detected, resetting inactivity timeout")
        handler.removeCallbacks(inactivityRunnable)
        handler.postDelayed(inactivityRunnable, inactivityTimeout)
    }

    private fun changeTemiFace() {
        Log.d("MainActivity", "changeTemiFace called")
        handler.removeCallbacks(inactivityRunnable) // Clear any existing callbacks
        startActivity(Intent(this, com.example.temiapp.ui.VideoActivity::class.java))
    }
    private fun hideTopBar() {
        robot.hideTopBar()
    }

    override fun onStart() {
        super.onStart()
        hideTopBar()
        robot.addOnRobotReadyListener(this)
        setupRecyclerView()
        fetchAndHandleProductsData()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }


    override fun onStop() {
        Log.e("MainActivity", "onStop called")
        if (isBoundRabbit) {
            unbindService(rabbitMQServiceConnection)
            isBoundRabbit = false
        }

        handler.removeCallbacks(inactivityRunnable) // Clean up
        super.onStop()

    }

    override fun onDestroy() {
        Log.e("MainActivity", "onDestroy called")
        handler.removeCallbacks(inactivityRunnable)
        if (isBoundRabbit) {
            unbindService(rabbitMQServiceConnection)
        }
        super.onDestroy()
    }
    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            val activityInfo = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
            robot.onStart(activityInfo)
            robot.speak(TtsRequest.create("Temi is ready", false))
        }
    }
}