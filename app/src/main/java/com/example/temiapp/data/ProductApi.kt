package com.example.temiapp.data

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import okhttp3.ResponseBody
import android.graphics.BitmapFactory

interface ProductApi {
    @GET("/api/products/data")
    fun getProductsData(): Call<List<Product>>
    @GET("/api/qrCodeImage/{id}")
    fun getQrCodeImage(@Path("id") productId: Int): Call<ResponseBody>
    @GET("/api/productImage/{id}")
    fun getProductImage(@Path("id") productId: Int): Call<ResponseBody>
    @GET("/api/products/detail/{id}")
    fun getProductDetail(@Path("id") productId: Int): Call<Product>
}