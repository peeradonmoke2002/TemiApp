package com.example.temiapp.network

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.arthenica.mobileffmpeg.FFmpeg

class RTSPStreamingManager(private val context: Context) {

    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private lateinit var cameraManager: CameraManager

    companion object {
        private const val TAG = "RTSPStreamingManager"
        private const val RTSP_URL = "rtsp://0.0.0.0:8554/stream"
    }

    fun startRTSPServer() {
        startBackgroundThread()
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0] // Access the first camera
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission issue: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error initializing camera: ${e.message}")
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread: ${e.message}")
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "Camera opened successfully")
            cameraDevice = camera
            startRTSPStream() // Start the stream after opening the camera
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "Camera disconnected")
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "Error opening camera: $error")
            camera.close()
            cameraDevice = null
        }
    }

    private fun startRTSPStream() {
        backgroundHandler?.post {
            if (cameraDevice == null) {
                Log.e(TAG, "Camera is not initialized. Cannot start RTSP stream.")
                return@post
            }

            // Prepare a SurfaceTexture for camera input
            val surfaceTexture = SurfaceTexture(10)
            val surface = Surface(surfaceTexture)

            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        captureRequestBuilder?.addTarget(surface)

                        session.setRepeatingRequest(captureRequestBuilder!!.build(), null, backgroundHandler)

                        // Start FFmpeg stream after configuring camera session
                        startFFmpegStream()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure the camera")
                    }
                },
                backgroundHandler
            )
        }
    }

    private fun startFFmpegStream() {
        val ffmpegCommand = arrayOf(
            "-f", "rawvideo",
            "-pix_fmt", "nv21", // Pixel format
            "-s", "1280x720", // Resolution
            "-i", "-",  // Input from pipe
            "-vcodec", "libx264",
            "-f", "rtsp",
            "-rtsp_transport", "tcp",
            RTSP_URL
        )

        Log.d(TAG, "Starting FFmpeg RTSP stream")
        FFmpeg.executeAsync(ffmpegCommand) { executionId, returnCode ->
            if (returnCode == 0) {
                Log.d(TAG, "FFmpeg RTSP stream started successfully")
            } else {
                Log.e(TAG, "Error starting FFmpeg RTSP stream. Code: $returnCode")
            }
        }
    }

    fun stopRTSPStream() {
        Log.d(TAG, "Stopping RTSP Stream")
        cameraCaptureSession?.close()
        cameraDevice?.close()
        cameraDevice = null

        // Stop FFmpeg
        FFmpeg.cancel()

        stopBackgroundThread()
        Log.d(TAG, "RTSP Stream stopped")
    }
}
