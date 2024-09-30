package com.example.temiapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.temiapp.data.ProductRepository
import com.example.temiapp.network.RabbitMQService
import com.example.temiapp.ui.ErrorActivity
import com.example.temiapp.ui.LoadingActivity
import com.example.temiapp.ui.ProductPageAdapter
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.listeners.OnMovementVelocityChangedListener
import com.robotemi.sdk.listeners.OnMovementStatusChangedListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.temiapp.utils.Utils
import kotlin.math.abs

class MainActivity : AppCompatActivity(), OnRobotReadyListener, OnMovementVelocityChangedListener, OnMovementStatusChangedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductPageAdapter
    private var rabbitMQService: RabbitMQService? = null
    private var isBoundRabbit = false
//    private val inactivityTimeout: Long = 1000 * 20 // 20 seconds timeout
//    private val handler = Handler(Looper.getMainLooper())
//    private val inactivityRunnable = Runnable { changeTemiFace() }
    private var isLoadingScreenActive = false
    private val productRepository = ProductRepository()
    private var changeTemiFaceStatus = false
    private var filter = IntentFilter("PRODUCT_UPDATED")
    private lateinit var robot: Robot

    private val rabbitMQServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RabbitMQService.RabbitBinder
            rabbitMQService = binder.getService()
            isBoundRabbit = true
            // Pass MainActivity reference to RabbitMQService
            rabbitMQService?.setMainActivity(this@MainActivity)
            Log.d("MainActivity", "RabbitMQService bound")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            rabbitMQService = null
            isBoundRabbit = false
            Log.d("MainActivity", "RabbitMQService unbound")
        }
    }

    // Send a message to a RabbitMQ queue
    private fun sendMessageToQueue(queueName: String, message: String) {
        if (isBoundRabbit && rabbitMQService != null) {
            rabbitMQService?.sendMessage(queueName, message)
            Log.d("MainActivity", "Message sent to $queueName: $message")
        } else {
            Log.e("MainActivity", "RabbitMQService is not bound")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the action bar
        supportActionBar?.hide()
        Utils.hideSystemBars(window)
        setContentView(R.layout.activity_main)
//        handler.postDelayed(inactivityRunnable, inactivityTimeout)
        // Initialize Robot
        robot = Robot.getInstance()
        robot.addOnRobotReadyListener(this)
        robot.addOnMovementVelocityChangedListener(this)
        robot.addOnMovementStatusChangedListener(this)
        // Bind to RabbitMQService
        bindService(Intent(this, RabbitMQService::class.java), rabbitMQServiceConnection, BIND_AUTO_CREATE)
        // Setup RecyclerView (this only needs to happen once)
        setupRecyclerView()
    }

    override fun onStart() {
         super.onStart()
         robot = Robot.getInstance()
         robot.addOnRobotReadyListener(this)
         robot.addOnMovementVelocityChangedListener(this)
         robot.addOnMovementStatusChangedListener(this)

         // will improve later!! TODO
         robot.tiltAngle(55, 1f)
         changeTemiFaceStatus = false
         robot.hideTopBar()
         Utils.hideSystemBars(window)
         registerReceiver(productUpdateReceiver, filter)

//         handler.postDelayed(inactivityRunnable, inactivityTimeout)
    }

    override fun onStop() {
        if (isBoundRabbit) {
            unbindService(rabbitMQServiceConnection)
            isBoundRabbit = false
        }
        robot.removeOnRobotReadyListener(this)
        robot.removeOnMovementVelocityChangedListener(this)
        robot.removeOnMovementStatusChangedListener(this)
        super.onStop()
    }

    override fun onDestroy() {
        if (isBoundRabbit) {
            unbindService(rabbitMQServiceConnection)
            rabbitMQService?.stopRabbitMQ()
        }
        robot.removeOnMovementStatusChangedListener(this)
        unregisterReceiver(productUpdateReceiver)
//        handler.removeCallbacks(inactivityRunnable)
        super.onDestroy()
    }


    fun resetInactivityTimeout() {
        // Remove any existing callbacks to inactivityRunnable
//        handler.removeCallbacks(inactivityRunnable)
//        // Post the inactivityRunnable again with the timeout duration
//        handler.postDelayed(inactivityRunnable, inactivityTimeout)
        Log.d("MainActivity", "Inactivity timeout reset do notting.")
    }


    // Setup RecyclerView
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        // Initialize the layout manager (horizontal scrolling)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Initialize the adapter with an empty list
        productAdapter = ProductPageAdapter(this, listOf())
        recyclerView.adapter = productAdapter
        // Attach PagerSnapHelper to snap items like a ViewPager
        if (recyclerView.onFlingListener == null) {
            PagerSnapHelper().attachToRecyclerView(recyclerView)
        }
        // Fetch and update data
        fetchAndHandleProductsData()
    }


    private fun fetchProductsData(onResult: (Boolean) -> Unit) {
        productRepository.getProductsData { products ->
            // Safely proceed only if products is not null and not empty
            products?.takeIf { it.isNotEmpty() }?.let {
                productAdapter.updateData(it.chunked(3))
                onResult(true)
            } ?: run {
                Log.e("MainActivity", "Failed to fetch products")
                onResult(false)
            }
        }
    }



    private fun fetchAndHandleProductsData() {
        Log.d("MainActivity", "Starting product data fetch")

        startLoadingScreen()

        fetchProductsData { isSuccess ->
            // Always run UI code on the main thread
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
        // Only start the loading screen if not already active
        if (isLoadingScreenActive) return

        isLoadingScreenActive = true
        Log.d("MainActivity", "Showing loading screen")

        // Launch the LoadingActivity
        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)
    }


    private fun closeLoadingScreen() {
        // Only close the loading screen if it's active
        if (!isLoadingScreenActive) return

        isLoadingScreenActive = false
        Log.d("MainActivity", "Closing loading screen")

        // Send a broadcast to finish the LoadingActivity
        val finishIntent = Intent("finish_loading_screen")
        sendBroadcast(finishIntent)
    }

    private fun showErrorScreen() {
        Log.d("MainActivity", "Launching error screen")

        // Launch the ErrorActivity and clear the current stack
        val intent = Intent(this, ErrorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)

        // Finish the current activity to avoid returning back to it
        finish()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        Log.d("MainActivity", "User interaction detected, resetting inactivity timeout")
        // reset the inactivity timeout
        resetInactivityTimeout()
    }

    private fun changeTemiFace() {
        Log.d("MainActivity", "changeTemiFace called")
//        handler.removeCallbacks(inactivityRunnable) // Clear any existing callbacks
        startActivity(Intent(this, com.example.temiapp.ui.VideoActivity::class.java))
    }

    override fun onMovementVelocityChanged(velocity: Float) {
        Log.d("MovementVelocityChanged", "Movement velocity: $velocity m/s")
        // Send velocity message to RabbitMQ queue
        sendMessageToQueue("robot_speed_queue", velocity.toString())

        // Define a small epsilon value for floating-point comparison
        val epsilon = 0.0001f

        // Check if velocity is not approximately zero and changeTemiFace_status is false
        if (kotlin.math.abs(velocity) > epsilon && !changeTemiFaceStatus) {
            Log.d("MainActivity", "User is moving, Change to video activity")
            lifecycleScope.launch {
                delay(500L)
                changeTemiFace()
                changeTemiFaceStatus = true
            }
        }
    }

    override fun onMovementStatusChanged(type: String, status: String) {
        Log.d("StatusChanged","Movement response - $type status: $status")

    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            try {
                val activityInfo = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
                robot.onStart(activityInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException(e)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshProductList() {
        productRepository.getProductsData { products ->
            if (products != null) {
                Log.d("MainActivity", "Refreshing product list")
                productAdapter.updateData(products.chunked(3))
                productAdapter.notifyDataSetChanged()
            } else {
                Log.e("MainActivity", "Failed to fetch products")
            }
        }
    }

    private val productUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MainActivity", "Received PRODUCT_UPDATED broadcast, refreshing product list")
            refreshProductList()
        }
    }
}
