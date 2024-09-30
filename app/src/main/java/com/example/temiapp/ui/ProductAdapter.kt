package com.example.temiapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.temiapp.R
import com.example.temiapp.data.Product
import com.example.temiapp.data.ProductRepository

// Helper class to group product views
private data class ProductViews(
    val nameView: TextView,
    val priceView: TextView,
    val imageView: ImageView,
    val buttonView: Button
)

class ProductPageAdapter(
    private val context: Context,
    private var productPages: List<List<Product>> // Pages of products
) : RecyclerView.Adapter<ProductPageAdapter.PageViewHolder>() {

    private val productRepository = ProductRepository()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        // Bind the product data for the current page
        val products = productPages[position]
        holder.bindProducts(products)
    }

    override fun getItemCount(): Int = productPages.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newPages: List<List<Product>>) {
        this.productPages = newPages
        notifyDataSetChanged()
    }

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Views for product items
        private val productViews = listOf(
            ProductViews(
                nameView = itemView.findViewById(R.id.product_name1),
                priceView = itemView.findViewById(R.id.product_price1),
                imageView = itemView.findViewById(R.id.product_image1),
                buttonView = itemView.findViewById(R.id.button1)
            ),
            ProductViews(
                nameView = itemView.findViewById(R.id.product_name2),
                priceView = itemView.findViewById(R.id.product_price2),
                imageView = itemView.findViewById(R.id.product_image2),
                buttonView = itemView.findViewById(R.id.button2)
            ),
            ProductViews(
                nameView = itemView.findViewById(R.id.product_name3),
                priceView = itemView.findViewById(R.id.product_price3),
                imageView = itemView.findViewById(R.id.product_image3),
                buttonView = itemView.findViewById(R.id.button3)
            )
        )

        private val errorMessage: TextView = itemView.findViewById(R.id.error_message)


        // Show "No Data" message or handle actual products
        fun bindProducts(products: List<Product?>) {
            hideAllProducts()
            if (products.isEmpty()) {
                // If the list is empty, show a "No Data" message
                showNoDataMessage()
            } else {
                // Bind products to views
                products.forEachIndexed { index, product ->
                    bindProduct(product, productViews[index])
                }
            }
        }

        // Generic product binding method
        @SuppressLint("SetTextI18n")
        private fun bindProduct(product: Product?, views: ProductViews) {
            if (product != null) {
                views.nameView.text = product.name
                views.priceView.text = "${product.price}"

                // Fetch and bind the image
                fetchProductImage(product.id, views.imageView)

                // Handle button click to show QR code
                views.buttonView.setOnClickListener {
                    showQRCode(product.id)
                }

                views.nameView.visibility = View.VISIBLE
                views.priceView.visibility = View.VISIBLE
                views.imageView.visibility = View.VISIBLE
                views.buttonView.visibility = View.VISIBLE
            }
        }

        // Hide all product views
        private fun hideAllProducts() {
            errorMessage.visibility = View.GONE
            productViews.forEach { views ->
                views.nameView.visibility = View.GONE
                views.priceView.visibility = View.GONE
                views.imageView.visibility = View.GONE
                views.buttonView.visibility = View.GONE
            }
        }

        // Show "No Data" message
        @SuppressLint("SetTextI18n")
        private fun showNoDataMessage() {
            productViews[0].nameView.apply {
                text = "No Data"
                visibility = View.VISIBLE
            }
            productViews[0].priceView.visibility = View.GONE
            productViews[0].imageView.visibility = View.GONE
        }

        private fun fetchProductImage(productId: Int, imageView: ImageView) {
            // Call the repository's method and handle the image fetching
            productRepository.getProductImage(productId) { responseBody ->
                if (responseBody != null) {
                    try {
                        val inputStream = responseBody.byteStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        imageView.setImageBitmap(bitmap)
                        inputStream.close() // Close the input stream to prevent leaks
                    } catch (e: Exception) {
                        Log.e("PageViewHolder", "Error decoding product image: ${e.localizedMessage}")
                        imageView.setImageResource(R.drawable.placeholder) // Set a placeholder image in case of error
                    }
                } else {
                    Log.e("PageViewHolder", "Failed to fetch image for product ID: $productId")
                    imageView.setImageResource(R.drawable.placeholder) // Set placeholder if fetching fails
                }
            }
        }

        // Navigate to QRCodeActivity
        private fun showQRCode(productId: Int) {
            try {
                val intent = Intent(context, QRCodeActivity::class.java).apply {
                    putExtra("PRODUCT_ID", productId)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("PageViewHolder", "Error showing QR code: ${e.localizedMessage}")
            }
        }
    }
}



