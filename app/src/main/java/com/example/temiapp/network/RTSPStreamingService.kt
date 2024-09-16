package com.example.temiapp.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.temiapp.MainActivity
import com.example.temiapp.R

class RTSPStreamingService : LifecycleService() {

    private lateinit var rtspServerManager: RTSPStreamingManager
    private val binder = RSTPBinder()

    companion object {
        private const val TAG = "RTSPStreamingService"
        const val CHANNEL_ID = "RTSPStreamingServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    // Binder class to return the instance of RTSPStreamingService
    inner class RSTPBinder : Binder() {
        fun getService(): RTSPStreamingService = this@RTSPStreamingService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RTSP Streaming Service started")

        rtspServerManager = RTSPStreamingManager(this)
        rtspServerManager.startRTSPServer() // Start the RTSP server
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: RTSP streaming service starting")
        super.onStartCommand(intent, flags, startId)

        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RTSP Streaming Service")
            .setContentText("Streaming Temi camera feed over RTSP")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "RTSP Streaming Service stopped")
        rtspServerManager.stopRTSPStream() // Stop the RTSP stream
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "RTSP Streaming Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }
}
