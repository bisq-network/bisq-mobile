package network.bisq.mobile.presentation.common.notification

import android.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.helper.ResourceUtils
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import org.koin.android.ext.android.get

/**
 * Implements foreground service (api >= 26) or background service accordingly
 *
 * This class is open for extension (for example, for the androidNode)
 *
 * android docs: https://developer.android.com/develop/background-work/services/foreground-services
 */
open class ForegroundService :
    Service(),
    Logging {
    companion object {
        const val SERVICE_NOTIF_ID = 1
        const val DEFAULT_NOTIFICATION_TITLE = "Bisq"
        const val DEFAULT_NOTIFICATION_TEXT = "Foreground Service Starting.."
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)

    private fun getServiceNotification(): Notification =
        try {
            val notificationController: NotificationControllerImpl = get()

            val contentPendingIntent =
                notificationController.createNavDeepLinkPendingIntent(NavRoute.TabOpenTradeList)

            NotificationCompat
                .Builder(this, NotificationChannels.BISQ_SERVICE)
                .setContentTitle("mobile.bisqService.title".i18n())
                .setContentText("mobile.bisqService.subTitle".i18n())
                .setSmallIcon(ResourceUtils.getNotifResId(applicationContext))
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .build()
        } catch (e: Exception) {
            log.e(e) { "Failed to create full service notification, falling back to minimal" }
            // Return a minimal notification as fallback
            NotificationCompat
                .Builder(this, NotificationChannels.BISQ_SERVICE)
                .setContentTitle("Bisq")
                .setContentText("Service running")
                .setSmallIcon(R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build()
        }

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        try {
            // Promote immediately with a minimal notification (no DI/i18n/deep links)
            val minimal =
                NotificationCompat
                    .Builder(this, NotificationChannels.BISQ_SERVICE)
                    .setContentTitle(DEFAULT_NOTIFICATION_TITLE)
                    .setContentText(DEFAULT_NOTIFICATION_TEXT)
                    .setSmallIcon(R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .build()

            ServiceCompat.startForeground(
                this,
                SERVICE_NOTIF_ID,
                minimal,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING,
            )
            log.i { "Foreground service promoted with minimal notification" }

            // Upgrade asynchronously to the full notification
            // This technique avoids some devices having ForegroundNotStartedInTime crashes
            serviceScope.launch {
                runCatching {
//                    uncomment to test default notification
//                    if (BuildNodeConfig.IS_DEBUG) {
//                        log.d { "Simulating slow device" }
//                        delay(10000L)
//                    }
                    val full = getServiceNotification()
                    ServiceCompat.startForeground(
                        this@ForegroundService,
                        SERVICE_NOTIF_ID,
                        full,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING,
                    )
                    log.d { "Foreground service notification upgraded via startForeground" }
                }.onFailure { e ->
                    log.w(e) { "Failed to upgrade foreground service notification; staying minimal" }
                }
            }
        } catch (e: Exception) {
            log.e(e) { "startForeground (minimal) failed; stopping service." }
            stopSelf()
            return
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        log.i { "Service starting sticky" }
        return START_STICKY
    }

    override fun onDestroy() {
        log.i { "Service is being destroyed" }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
