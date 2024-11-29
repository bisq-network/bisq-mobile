package network.bisq.mobile.domain.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Implements foreground service (api >= 26) or background service accordingly
 *
 * This class is open for extension (for example, for the androidNode)
 *
 * android docs: https://developer.android.com/develop/background-work/services/foreground-services
 */
open class BisqForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "BISQ_SERVICE_CHANNEL"
        const val SERVICE_ID = 21000000
        const val SERVICE_NAME = "Bisq Foreground Service"
        const val PUSH_NOTIFICATION_ACTION_KEY = "network.bisq.bisqapps.ACTION_REQUEST_PERMISSION"
    }

    override fun onCreate() {
        super.onCreate()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(SERVICE_NAME)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // For android previous to O
            .build()
        startForeground(SERVICE_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check if notification permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            // Send a broadcast to the activity to request permission
            val broadcastIntent = Intent(PUSH_NOTIFICATION_ACTION_KEY)
            sendBroadcast(broadcastIntent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup tasks
    }

    override fun onBind(intent: Intent?): IBinder? = null
}