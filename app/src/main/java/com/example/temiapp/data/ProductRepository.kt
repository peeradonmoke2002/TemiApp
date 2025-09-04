package com.example.temiapp.data

import com.example.temiapp.network.RetrofitClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductRepository {

    private val productApi: ProductApi = RetrofitClient.retrofit.create(ProductApi::class.java)


    private val productList = mutableListOf<Product>()


    fun getProductsData(callback: (List<Product>?) -> Unit) {
        val call = productApi.getProductsData()

        call.enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful) {

                    val products = response.body()
                    productList.clear()
                    products?.let { productList.addAll(it) }
                    callback(products)
                } else {

                    callback(null)
                }
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {

                callback(null)
            }
        })
    }

    fun getProductDetails(productId: Int, callback: (Product?) -> Unit) {
        val call = productApi.getProductDetail(productId)
        call.enqueue(object : Callback<Product> {
            override fun onResponse(call: Call<Product>, response: Response<Product>) {
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<Product>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun getProductImage(productId: Int, callback: (ResponseBody?) -> Unit) {
        val call = productApi.getProductImage(productId)

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