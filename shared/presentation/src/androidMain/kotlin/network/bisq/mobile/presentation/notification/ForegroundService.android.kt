package network.bisq.mobile.presentation.notification

import android.annotation.SuppressLint
import android.app.Notification
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
    }

    private val notificationController: NotificationControllerImpl by inject()

    private fun getServiceNotification(): Notification {
        val contentPendingIntent =
            notificationController.createNavDeepLinkPendingIntent(Routes.TabOpenTradeList)

        return NotificationCompat.Builder(this, NotificationChannels.BISQ_SERVICE)
            .setContentTitle("mobile.bisqService.title".i18n())
            .setContentText("mobile.bisqService.subTitle".i18n())
            .setSmallIcon(ResourceUtils.getNotifResId(applicationContext))
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
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
        log.i { "Service starting sticky" }
        return START_STICKY
    }

    override fun onDestroy() {
        log.i { "Service is being destroyed" }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
