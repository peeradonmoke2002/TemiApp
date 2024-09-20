package com.example.temiapp.data

import com.example.temiapp.network.RetrofitClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductRepository {

    private val productApi: ProductApi = RetrofitClient.retrofit.create(ProductApi::class.java)

    // Placeholder for an in-memory product list (can be replaced with database or API calls)
    private val productList = mutableListOf<Product>()

    // Fetch product data from the API
    fun getProductsData(callback: (List<Product>?) -> Unit) {
        val call = productApi.getProductsData()

        call.enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful) {
                    // Update the productList and pass the data to the callback
                    val products = response.body()
                    productList.clear()
                    products?.let { productList.addAll(it) }
                    callback(products)
                } else {
                    // If the response fails, pass null to the callback
                    callback(null)
                }
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                // If the call fails, pass null to the callback
                callback(null)
            }
        })
    }

    // Fetch product image from the API
    fun getProductImage(productId: Int, callback: (ResponseBody?) -> Unit) {
        val call = productApi.getProductImage(productId)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    callback(response.body())  // Pass the ResponseBody to the callback
                } else {
                    callback(null)  // Pass null if the response is not successful
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback(null)  // Pass null if there's an error
            }
        })
    }

    // Fetch QR code image from the API
    fun getQrCodeImage(productId: Int, callback: (ResponseBody?) -> Unit) {
        val call = productApi.getQrCodeImage(productId)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback(null)
            }
        })
    }





}
