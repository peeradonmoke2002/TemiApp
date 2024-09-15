package com.example.temiapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.temiapp.R
import com.example.temiapp.data.Product
import com.example.temiapp.data.ProductApi
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
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
    }

    override fun getItemCount(): Int = if (hasError || dataIsEmpty) 1 else productPages.size

    // Update the adapter with new data
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newPages: List<List<Product>>) {
        productPages = newPages
        notifyDataSetChanged()
    }

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Error or No Data message TextView
        private val errorMessage: TextView = itemView.findViewById(R.id.error_message)

        // Product Views for first product
        val productName1: TextView = itemView.findViewById(R.id.product_name1)
        val productPrice1: TextView = itemView.findViewById(R.id.product_price1)
        val productImage1: ImageView = itemView.findViewById(R.id.product_image1)
        val buttonProduct1: Button = itemView.findViewById(R.id.button1)

        // Product Views for second product
        val productName2: TextView = itemView.findViewById(R.id.product_name2)
        val productPrice2: TextView = itemView.findViewById(R.id.product_price2)
        val productImage2: ImageView = itemView.findViewById(R.id.product_image2)
        val buttonProduct2: Button = itemView.findViewById(R.id.button2)

        // Product Views for third product
        val productName3: TextView = itemView.findViewById(R.id.product_name3)
        val productPrice3: TextView = itemView.findViewById(R.id.product_price3)
        val productImage3: ImageView = itemView.findViewById(R.id.product_image3)
        val buttonProduct3: Button = itemView.findViewById(R.id.button3)

        // Show error message in the center
        fun showErrorMessage(message: String) {
            hideAllProducts()
            errorMessage.visibility = View.VISIBLE
            errorMessage.text = message
        }

        // Show "No Data" message in the center
        fun showNoDataMessage() {
            hideAllProducts()
            productName1.text = "No Data"
            productPrice1.text = ""
            productImage1.visibility = View.GONE
        }

        // Generic product binding method
        fun bindProduct(product: Product?, productName: TextView, productPrice: TextView, productImage: ImageView, buttonProduct: Button) {
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
        }

        // Helper method to hide all product views
        private fun hideAllProducts() {
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
        }

        // Fetch product image and bind it to the corresponding ImageView
        private fun fetchProductImage(productId: Int, imageView: ImageView) {
            val productApi = RetrofitClient.retrofit.create(ProductApi::class.java)
            productApi.getProductImage(productId).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        response.body()?.let { responseBody ->
                            val inputStream = responseBody.byteStream()
                            val bitmap = BitmapFactory.decodeStream(inputStream)

                            // Display the bitmap in the ImageView
                            imageView.setImageBitmap(bitmap)
                        }
                    } else {
                        // Handle image fetch failure
                        imageView.setImageResource(R.drawable.placeholder) // Set placeholder if needed
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    // Handle failure (e.g., show placeholder image)
                    imageView.setImageResource(R.drawable.placeholder)
                }
            })
        }

        // Navigate to QRCodeActivity
        private fun showQRCode(productId: Int) {
            val intent = Intent(context, QRCodeActivity::class.java)
            intent.putExtra("PRODUCT_ID", productId)
            context.startActivity(intent)
        }


    }
}
