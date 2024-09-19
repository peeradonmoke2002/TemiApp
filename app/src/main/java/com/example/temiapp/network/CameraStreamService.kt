package com.example.temiapp.network

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraStreamService : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var webSocketServer: WebSocketServer

    override fun onCreate() {
        super.onCreate()

        // Initialize the executor for handling camera frame analysis
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize WebSocket server on port 8765 and start it
        webSocketServer = WebSocketServer(8765)
        webSocketServer.startServer()

        // Start camera if permission is already granted (MainActivity should handle permission checking)
        startCamera()
    }

    private fun startCamera() {
        // Get a camera provider instance for this context
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Configure image analyzer to process camera frames
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, { imageProxy -> analyzeFrame(imageProxy) })
                }

            try {
                // Ensure that no other camera use cases are bound
                cameraProvider.unbindAll()

                // Bind the camera lifecycle and start analyzing frames
                cameraProvider.bindToLifecycle(
                    this, // LifecycleOwner from LifecycleService
                    cameraSelector,
                    imageAnalyzer
                )

                Log.d("CameraStreamService", "Camera started successfully")

            } catch (exc: Exception) {
                Log.e("CameraStreamService", "Error starting camera: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        // Get the image data from the proxy's buffer
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Send the frame data via WebSocket to connected clients
        webSocketServer.broadcastFrame(bytes)

        // Release the imageProxy resource after processing the frame
        imageProxy.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Properly shut down the camera executor when the service is destroyed
        cameraExecutor.shutdown()

        // Stop the WebSocket server
        webSocketServer.stopServer()

        Log.d("CameraStreamService", "Camera service stopped")
    }
}
