package com.example.temiapp

import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductPageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view)

        // Set up LinearLayoutManager with horizontal scrolling for row-based product display
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager

        // Initialize the RecyclerView with an empty list to avoid warnings
        productAdapter = ProductPageAdapter(this, listOf())
        recyclerView.adapter = productAdapter

        // Attach PagerSnapHelper for page-like swiping (3 products per page)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // Fetch products and update the adapter when data is available
        fetchProducts()
    }

    private fun fetchProducts() {
        val productApi = RetrofitClient.retrofit.create(ProductApi::class.java)

        productApi.getProducts().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful) {
                    // Log the raw response to check what the API is returning
                    Log.d("MainActivity", "Raw Response: ${response.body().toString()}")

                    response.body()?.let {
                        // Split the product list into pages (each page with up to 3 products)
                        val productPages = it.chunked(3)

                        // Update the adapter with the actual paginated data
                        productAdapter = ProductPageAdapter(this@MainActivity, productPages)
                        recyclerView.adapter = productAdapter
                    }
                } else {
                    Log.e("MainActivity", "Response unsuccessful")
                }
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                Log.e("MainActivity", "API call failed", t)
            }
        })
    }
}
