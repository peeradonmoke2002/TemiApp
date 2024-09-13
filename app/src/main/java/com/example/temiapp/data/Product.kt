package com.example.temiapp.data

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val detail: String,
    var productImage: ProductImage? = null
)
data class ProductImage(
    val imageData: String? = null
)
