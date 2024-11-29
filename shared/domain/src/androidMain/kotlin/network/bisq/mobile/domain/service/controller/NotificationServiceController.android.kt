package network.bisq.mobile.domain.service.controller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import network.bisq.mobile.domain.service.BisqForegroundService

/**
 * Controller interacting with the bisq service
 */
actual class NotificationServiceController (private val context: Context): ServiceController {

    companion object {
        const val SERVICE_NAME = "Bisq Service"
    }
    private var isRunning = false

    /**
     * Starts the service in the appropiate mode based on the current device running Android API
     */
    actual override fun startService() {
        if (!isRunning) {
            createNotificationChannel()
            val intent = Intent(context, BisqForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                // if the phone does not support foreground service
                context.startService(intent)
            }
            isRunning = true
        }
    }

    actual override fun stopService() {
        // TODO we need to leave the service running if the user is ok with it
        deleteNotificationChannel()
        val intent = Intent(context, BisqForegroundService::class.java)
        context.stopService(intent)
        isRunning = false
    }

    actual fun pushNotification(title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, BisqForegroundService.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // For android previous to O
            .build()
        notificationManager.notify(1, notification)
    }

    actual override fun isServiceRunning() = isRunning

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(BisqForegroundService.CHANNEL_ID, SERVICE_NAME, NotificationManager.IMPORTANCE_LOW)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun deleteNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel(BisqForegroundService.CHANNEL_ID)
        }
    }
}