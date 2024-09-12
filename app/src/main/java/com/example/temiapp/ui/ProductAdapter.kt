package com.example.temiapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.temiapp.R
import com.example.temiapp.data.Product

// Adapter to handle pages of products, each page having 3 products
class ProductPageAdapter(private val context: Context, private val productPages: List<List<Product>>) :
    RecyclerView.Adapter<ProductPageAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val products = productPages[position]

        // Bind the first product if available, else hide the views
        if (products.isNotEmpty()) {
            holder.productName1.text = products[0].name
            holder.productPrice1.text = "${products[0].price} Bath"
            val bitmap1 = decodeBase64(products[0].product_image)
            holder.productImage1.setImageBitmap(bitmap1)
            holder.setVisibilityForProduct1(View.VISIBLE)
        } else {
            holder.setVisibilityForProduct1(View.GONE) // Hide if no data
        }

        // Bind the second product if available, else hide the views
        if (products.size > 1) {
            holder.productName2.text = products[1].name
            holder.productPrice2.text = "${products[1].price} Bath"
            val bitmap2 = decodeBase64(products[1].product_image)
            holder.productImage2.setImageBitmap(bitmap2)
            holder.setVisibilityForProduct2(View.VISIBLE)
        } else {
            holder.setVisibilityForProduct2(View.GONE) // Hide if no data
        }

        // Bind the third product if available, else hide the views
        if (products.size > 2) {
            holder.productName3.text = products[2].name
            holder.productPrice3.text = "${products[2].price} Bath"
            val bitmap3 = decodeBase64(products[2].product_image)
            holder.productImage3.setImageBitmap(bitmap3)
            holder.setVisibilityForProduct3(View.VISIBLE)
        } else {
            holder.setVisibilityForProduct3(View.GONE) // Hide if no data
        }
    }

    override fun getItemCount(): Int = productPages.size

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // First product views
        val productName1: TextView = itemView.findViewById(R.id.product_name1)
        val productPrice1: TextView = itemView.findViewById(R.id.product_price1)
        val productImage1: ImageView = itemView.findViewById(R.id.product_image1)

        // Second product views
        val productName2: TextView = itemView.findViewById(R.id.product_name2)
        val productPrice2: TextView = itemView.findViewById(R.id.product_price2)
        val productImage2: ImageView = itemView.findViewById(R.id.product_image2)

        // Third product views
        val productName3: TextView = itemView.findViewById(R.id.product_name3)
        val productPrice3: TextView = itemView.findViewById(R.id.product_price3)
        val productImage3: ImageView = itemView.findViewById(R.id.product_image3)

        // Helper methods to hide or show the views for each product
        fun setVisibilityForProduct1(visibility: Int) {
            productName1.visibility = visibility
            productPrice1.visibility = visibility
            productImage1.visibility = visibility
        }

        fun setVisibilityForProduct2(visibility: Int) {
            productName2.visibility = visibility
            productPrice2.visibility = visibility
            productImage2.visibility = visibility
        }

        fun setVisibilityForProduct3(visibility: Int) {
            productName3.visibility = visibility
            productPrice3.visibility = visibility
            productImage3.visibility = visibility
        }
    }

    // Function to decode base64 string into Bitmap
    private fun decodeBase64(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
