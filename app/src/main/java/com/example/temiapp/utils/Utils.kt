package com.example.temiapp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

object Utils {
    fun decodeBase64ToBitmap(base64Str: String): Bitmap {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
}