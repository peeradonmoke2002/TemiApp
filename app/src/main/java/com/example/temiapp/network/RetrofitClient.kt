package com.example.temiapp.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.temiapp.config.Config

object RetrofitClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)  // Set connection timeout
        .readTimeout(1, TimeUnit.SECONDS)     // Set read timeout
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Config.apiBaseUrl)  // Dynamically use the API base URL from Config
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}