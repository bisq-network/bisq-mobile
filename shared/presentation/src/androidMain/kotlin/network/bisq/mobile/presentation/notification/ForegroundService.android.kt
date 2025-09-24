package network.bisq.mobile.presentation.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import network.bisq.mobile.domain.helper.ResourceUtils
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.navigation.Routes
import org.koin.android.ext.android.inject

/**
 * Implements foreground service (api >= 26) or background service accordingly
 *
 * This class is open for extension (for example, for the androidNode)
 *
 * android docs: https://developer.android.com/develop/background-work/services/foreground-services
 */
open class ForegroundService : Service(), Logging {
    companion object {
        const val SERVICE_NOTIF_ID = 1
        const val STOP_SERVICE_ACTION = "STOP_SERVICE_ACTION"
    }

    private val notificationController: NotificationControllerImpl by inject()

    private fun getServiceNotification(): Notification {
        val contentPendingIntent =
            notificationController.createNavDeepLinkPendingIntent(Routes.TabOpenTradeList)

        val stopIntent = Intent(applicationContext, ForegroundService::class.java).apply {
            action = STOP_SERVICE_ACTION
        }
        val stopPendingIntent = PendingIntent.getService(
            applicationContext,
            SERVICE_NOTIF_ID + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, AndroidNotificationChannels.BISQ_SERVICE_CHANNEL_ID)
            .setContentTitle("mobile.bisqService.title".i18n())
            .setContentText("mobile.bisqService.subTitle".i18n())
            .setSmallIcon(ResourceUtils.getNotifResId(applicationContext))
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "mobile.action.notifications.stop".i18n(),
                    stopPendingIntent
                )
            )
            .build()
    }

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        // ServiceCompat impl. checks for android versions internally
        ServiceCompat.startForeground(
            this,
            SERVICE_NOTIF_ID,
            getServiceNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
        )
        log.i { "Started as foreground service" }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_SERVICE_ACTION) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            log.i { "Notification foreground service stopped by user" }
            stopSelf()
            return START_NOT_STICKY
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
