package network.bisq.mobile.domain.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.utils.Logging

/**
 * Implements foreground service (api >= 26) or background service accordingly
 *
 * This class is open for extension (for example, for the androidNode)
 *
 * android docs: https://developer.android.com/develop/background-work/services/foreground-services
 */
open class BisqForegroundService : Service(), Logging {
    companion object {
        const val CHANNEL_ID = "BISQ_SERVICE_CHANNEL"
        const val SERVICE_ID = 21000000
        const val PUSH_NOTIFICATION_ID = 1
        const val SERVICE_NAME = "Bisq Foreground Service"
        const val PUSH_NOTIFICATION_ACTION_KEY = "network.bisq.bisqapps.ACTION_REQUEST_PERMISSION"
    }

    private lateinit var silentNotification: Notification

    private lateinit var defaultNotification: Notification

    override fun onCreate() {
        super.onCreate()
        initDefaultNotifications()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(this, SERVICE_ID, silentNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
            log.i { "Started as foreground service compat"}
        } else {
            startForeground(SERVICE_ID, silentNotification)
            log.i { "Started foreground"}
        }
        log.i { "Service ready" }

        CoroutineScope(Dispatchers.Main).launch {
            delay(10000)  // Wait for 10 seconds
            // Create an Intent to open the MainActivity when the notification is tapped
            val pendingIntent = null
//            val intent = Intent(this, MainActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clears any existing task and starts a new one
//            }
//
//            // Create a PendingIntent to wrap the Intent
//            val pendingIntent: PendingIntent = PendingIntent.getActivity(
//                this@BisqForegroundService,
//                0,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT // This flag updates the existing PendingIntent if it's already created
//            )
            val updatedNotification: Notification = NotificationCompat.Builder(this@BisqForegroundService, CHANNEL_ID)
                .setContentTitle("New Update!")
                .setContentText("Tap to open the app")
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)  // Keeps the notification active
                .setContentIntent(pendingIntent)  // Set the pending intent to launch the app
                .build()

            // Update the notification
//            NotificationManagerCompat.from(this@BisqForegroundService).notify(SERVICE_ID, updatedNotification)

            val notificationManager = this@BisqForegroundService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(PUSH_NOTIFICATION_ID, updatedNotification)

            // Log the update
            log.i { "Notification updated after 10 seconds" }
        }
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
        log.i { "Service starting sticky" }
        return START_STICKY
    }

    override fun onDestroy() {
        log.i { "Service is being destroyed" }
        super.onDestroy()
        // Cleanup tasks
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initDefaultNotifications() {
        silentNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("") // No title
            .setContentText("")  // No content
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setPriority(NotificationCompat.PRIORITY_MIN)  // Silent notification
            .setOngoing(true)  // Keeps the notification active
            .build()
        defaultNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(SERVICE_NAME)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // For android previous to O
            .build()
    }
}