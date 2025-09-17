package network.bisq.mobile.domain.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import network.bisq.mobile.domain.helper.ResourceUtils
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController.Companion.EXTRA_DESTINATION
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import org.koin.android.ext.android.inject

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
        const val REQUEST_CODE = 21000001
        const val PUSH_NOTIFICATION_ID = 1
    }

    private val notificationServiceController: NotificationServiceController by inject()

    private fun getServiceNotification(): Notification {
        // Create an intent that brings the user back to the app
        val intent = Intent(applicationContext, notificationServiceController.activityClassForIntents).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION, "tab_my_trades") // Add extras to navigate to a specific screen
        }

        // Create a PendingIntent to handle the notification click
        // Use unique request code to avoid PendingIntent conflicts
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("mobile.bisqService.title".i18n())
            .setContentText("mobile.bisqService.subTitle".i18n())
            .setSmallIcon(ResourceUtils.getNotifResId(applicationContext))
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        // ServiceCompat impl. checks for android versions internally
        ServiceCompat.startForeground(
            this,
            SERVICE_ID,
            getServiceNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
        )
        log.i { "Started as foreground service" }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android API 35
        if (intent?.action == "DISMISS_NOTIFICATION") {
            val notificationId = intent.getIntExtra("notificationId", PUSH_NOTIFICATION_ID)
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            notificationManager.cancel(notificationId)
            log.i { "Notification $notificationId dismissed by user" }
            return START_STICKY
        }
        log.i { "Service starting sticky" }
        return START_STICKY
    }

    override fun onDestroy() {
        log.i { "Service is being destroyed" }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}