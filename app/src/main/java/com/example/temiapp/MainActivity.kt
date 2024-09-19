package com.example.temiapp

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.temiapp.data.Product
import com.example.temiapp.data.ProductApi
import com.example.temiapp.network.RabbitMQService
import com.example.temiapp.network.RetrofitClient
import com.example.temiapp.ui.ErrorActivity
import com.example.temiapp.ui.LoadingActivity
import com.example.temiapp.ui.ProductPageAdapter
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnRobotReadyListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(), OnRobotReadyListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductPageAdapter
    private lateinit var robot: Robot

    private var rabbitMQService: RabbitMQService? = null
    private var isBoundRabbit = false
    private val handler = Handler(Looper.getMainLooper())
    private val inactivityTimeout: Long = 30000 // 30 seconds timeout
    private val inactivityRunnable = Runnable { changeTemiFace() }

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
        setContentView(R.layout.activity_main)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        // Initialize Temi Robot
        robot = Robot.getInstance()

        hideSystemBars()

        // Start inactivity timeout
        handler.postDelayed(inactivityRunnable, inactivityTimeout)

        // Bind to RabbitMQService
        bindService(Intent(this, RabbitMQService::class.java), rabbitMQServiceConnection, BIND_AUTO_CREATE)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        productAdapter = ProductPageAdapter(this, listOf(), hasError = false, dataIsEmpty = false)
        recyclerView.adapter = productAdapter

        // Attach PagerSnapHelper only if it is not already attached
        if (recyclerView.onFlingListener == null) {
            PagerSnapHelper().attachToRecyclerView(recyclerView)
        }
    }


    private fun fetchProductsData(onResult: (Boolean) -> Unit) {
        val productApi = RetrofitClient.retrofit.create(ProductApi::class.java)
        productApi.getProductsData().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                val products = response.body() ?: listOf()
                if (products.isEmpty()) {
                    onResult(false) // Handle empty data case
                } else {
                    productAdapter.updateData(products.chunked(3)) // Split into pages of 3 products
                    onResult(true)
                }
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        })
    }

    private fun fetchAndHandleProductsData() {
        startLoadingScreen()
        fetchProductsData { isSuccess ->
            runOnUiThread {
                handler.postDelayed({
                    if (isSuccess) {
                        sendBroadcast(Intent(LoadingActivity::class.java.name).apply { action = "finish_loading" })
                    } else {
                        showErrorScreen()
                    }
                }, 1000)
            }
        }
    }

    private fun startLoadingScreen() {
        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)
    }

    private fun showErrorScreen() {
        val intent = Intent(this, ErrorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
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
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        handler.removeCallbacks(inactivityRunnable)
        handler.postDelayed(inactivityRunnable, inactivityTimeout)
    }

    private fun changeTemiFace() {
        startActivity(Intent(this, com.example.temiapp.ui.VideoActivity::class.java))
    }

    override fun onStart() {
        super.onStart()
        hideTopBar()
        robot.addOnRobotReadyListener(this)
        setupRecyclerView()
        fetchAndHandleProductsData()
    }

    override fun onStop() {
        super.onStop()
        robot.removeOnRobotReadyListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBoundRabbit) {
            unbindService(rabbitMQServiceConnection)
            isBoundRabbit = false
        }
        handler.removeCallbacks(inactivityRunnable)
    }

    private fun hideTopBar() {
        robot.hideTopBar()
    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            val activityInfo = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
            robot.onStart(activityInfo)
            robot.speak(TtsRequest.create("Temi is ready", false))
        }
    }
}
