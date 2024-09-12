package com.example.temiapp.data

import retrofit2.Call
import retrofit2.http.GET

interface ProductApi {
    @GET("/api/products")
    fun getProducts(): Call<List<Product>>
}