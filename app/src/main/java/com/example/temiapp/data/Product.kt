package com.example.temiapp.data

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val product_image: String,  // Expecting base64-encoded image
    val qr_code_image: String,  // Expecting base64-encoded image
    val detail: String
)