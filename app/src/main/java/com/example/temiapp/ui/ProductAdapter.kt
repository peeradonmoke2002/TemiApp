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
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.temiapp.R
import com.example.temiapp.data.Product
import com.example.temiapp.data.ProductApi
import com.example.temiapp.data.ProductRepository
import com.example.temiapp.network.RetrofitClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductPageAdapter(
    private val context: Context,
    private var productPages: List<List<Product>>, // Pages of products
    private val hasError: Boolean,                 // Flag to indicate an error
    private val dataIsEmpty: Boolean               // Flag to indicate no data
) : RecyclerView.Adapter<ProductPageAdapter.PageViewHolder>() {

    private val productRepository = ProductRepository()

    fun getProductPages(): List<List<Product>> {
        return productPages
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        return try {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
            PageViewHolder(view)
        } catch (e: Exception) {
            Log.e("ProductPageAdapter", "Error inflating view: ${e.localizedMessage}")
            throw e
        }
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        try {
            when {
                hasError -> holder.showErrorMessage("Error can't connect to server. Please try again.")
                dataIsEmpty -> holder.showNoDataMessage()
                else -> {
                    val products = productPages[position]

                    // Bind products (up to 3 per page)
                    holder.bindProduct(products.getOrNull(0), holder.productName1, holder.productPrice1, holder.productImage1, holder.buttonProduct1)
                    holder.bindProduct(products.getOrNull(1), holder.productName2, holder.productPrice2, holder.productImage2, holder.buttonProduct2)
                    holder.bindProduct(products.getOrNull(2), holder.productName3, holder.productPrice3, holder.productImage3, holder.buttonProduct3)
                }
            }
        } catch (e: Exception) {
            Log.e("ProductPageAdapter", "Error binding view at position $position: ${e.localizedMessage}")
        }
    }

    override fun getItemCount(): Int = if (hasError || dataIsEmpty) 1 else productPages.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newPages: List<List<Product>>) {
        try {
            productPages = newPages
            notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("ProductPageAdapter", "Error updating data: ${e.localizedMessage}")
        }
    }

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Views for the product items
        val productName1: TextView = itemView.findViewById(R.id.product_name1)
        val productPrice1: TextView = itemView.findViewById(R.id.product_price1)
        val productImage1: ImageView = itemView.findViewById(R.id.product_image1)
        val buttonProduct1: Button = itemView.findViewById(R.id.button1)

        val productName2: TextView = itemView.findViewById(R.id.product_name2)
        val productPrice2: TextView = itemView.findViewById(R.id.product_price2)
        val productImage2: ImageView = itemView.findViewById(R.id.product_image2)
        val buttonProduct2: Button = itemView.findViewById(R.id.button2)

        val productName3: TextView = itemView.findViewById(R.id.product_name3)
        val productPrice3: TextView = itemView.findViewById(R.id.product_price3)
        val productImage3: ImageView = itemView.findViewById(R.id.product_image3)
        val buttonProduct3: Button = itemView.findViewById(R.id.button3)

        // Error or No Data message TextView
        private val errorMessage: TextView = itemView.findViewById(R.id.error_message)

        // Show error message in the center
        fun showErrorMessage(message: String) {
            try {
                hideAllProducts()
                errorMessage.visibility = View.VISIBLE
                errorMessage.text = message
            } catch (e: Exception) {
                Log.e("PageViewHolder", "Error showing error message: ${e.localizedMessage}")
            }
        }

        // Show "No Data" message in the center
        fun showNoDataMessage() {
            try {
                hideAllProducts()
                productName1.text = "No Data"
                productPrice1.text = ""
                productImage1.visibility = View.GONE
            } catch (e: Exception) {
                Log.e("PageViewHolder", "Error showing no data message: ${e.localizedMessage}")
            }
        }

        // Generic product binding method
        fun bindProduct(product: Product?, productName: TextView, productPrice: TextView, productImage: ImageView, buttonProduct: Button) {
            try {
                if (product != null) {
                    productName.text = product.name
                    productPrice.text = "${product.price} Bath"

                    // Fetch and bind the image
                    fetchProductImage(product.id, productImage)

                    // Handle button click to show QR code
                    buttonProduct.setOnClickListener {
                        showQRCode(product.id)
                    }

                    productName.visibility = View.VISIBLE
                    productPrice.visibility = View.VISIBLE
                    productImage.visibility = View.VISIBLE
                    buttonProduct.visibility = View.VISIBLE
                } else {
                    productName.visibility = View.GONE
                    productPrice.visibility = View.GONE
                    productImage.visibility = View.GONE
                    buttonProduct.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("PageViewHolder", "Error binding product: ${e.localizedMessage}")
            }
        }

        // Helper method to hide all product views
        private fun hideAllProducts() {
            try {
                productName1.visibility = View.GONE
                productPrice1.visibility = View.GONE
                productImage1.visibility = View.GONE
                buttonProduct1.visibility = View.GONE

                productName2.visibility = View.GONE
                productPrice2.visibility = View.GONE
                productImage2.visibility = View.GONE
                buttonProduct2.visibility = View.GONE

                productName3.visibility = View.GONE
                productPrice3.visibility = View.GONE
                productImage3.visibility = View.GONE
                buttonProduct3.visibility = View.GONE
            } catch (e: Exception) {
                Log.e("PageViewHolder", "Error hiding products: ${e.localizedMessage}")
            }
        }


        private fun fetchProductImage(productId: Int, imageView: ImageView) {
            // Call the repository's method and handle the image fetching
            productRepository.getProductImage(productId) { responseBody ->
                if (responseBody != null) {
                    try {
                        val inputStream = responseBody.byteStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        imageView.setImageBitmap(bitmap)
                        inputStream.close()  // Close the input stream to prevent leaks
                    } catch (e: Exception) {
                        Log.e("PageViewHolder", "Error decoding product image: ${e.localizedMessage}")
                        imageView.setImageResource(R.drawable.placeholder)  // Set a placeholder image in case of error
                    }
                } else {
                    Log.e("PageViewHolder", "Failed to fetch image for product ID: $productId")
                    imageView.setImageResource(R.drawable.placeholder)  // Set placeholder if fetching fails
                }
            }
        }





        // Navigate to QRCodeActivity
        private fun showQRCode(productId: Int) {
            try {
                val intent = Intent(context, QRCodeActivity::class.java)
                intent.putExtra("PRODUCT_ID", productId)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("PageViewHolder", "Error showing QR code: ${e.localizedMessage}")
            }
        }
    }
}
