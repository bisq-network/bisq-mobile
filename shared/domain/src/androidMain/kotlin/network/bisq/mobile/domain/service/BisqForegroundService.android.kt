package network.bisq.mobile.domain.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.helper.getNotifResId
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

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
        initDefaultNotifications(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(this, SERVICE_ID, silentNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
            log.i { "Started as foreground service compat"}
        } else {
            startForeground(SERVICE_ID, silentNotification)
            log.i { "Started foreground"}
        }
        log.i { "Service ready" }

        // Update the foreground service notification with proper text after a short delay
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)

            val serviceNotification: Notification = NotificationCompat.Builder(this@BisqForegroundService, CHANNEL_ID)
                .setSmallIcon(getNotifResId(applicationContext))
                .setContentTitle("mobile.bisqService.title".i18n())
                .setContentText("mobile.bisqService.subTitle".i18n())
                .setPriority(NotificationCompat.PRIORITY_LOW) // Low priority for service notification
                .setOngoing(true)  // Keeps the notification active
                .build()

            // Update the SAME notification ID to replace the silent one
            val notificationManager = this@BisqForegroundService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(SERVICE_ID, serviceNotification) // Use SERVICE_ID, not PUSH_NOTIFICATION_ID

            log.i { "Service notification updated with text" }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android API 35
        if (intent?.action == "DISMISS_NOTIFICATION") {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = intent.getIntExtra("notificationId", PUSH_NOTIFICATION_ID)
            notificationManager.cancel(notificationId)
            log.i { "Notification $notificationId dismissed by user" }
            return START_STICKY
        }
        
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

    private fun initDefaultNotifications(context: Context) {
        silentNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("") // No title
            .setContentText("")  // No content
            .setSmallIcon(getNotifResId(context))
            .setPriority(NotificationCompat.PRIORITY_MIN)  // Silent notification
            .setOngoing(true)  // Keeps the notification active
            .build()
        defaultNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(SERVICE_NAME)
            .setSmallIcon(getNotifResId(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // For android previous to O
            .build()
    }
}