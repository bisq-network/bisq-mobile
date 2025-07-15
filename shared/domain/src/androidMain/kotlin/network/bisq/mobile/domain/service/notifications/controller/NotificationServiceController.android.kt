package network.bisq.mobile.domain.service.notifications.controller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.app.ActivityOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.BisqForegroundService
import network.bisq.mobile.domain.utils.Logging

/**
 * Controller interacting with the bisq service
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class NotificationServiceController (private val appForegroundController: AppForegroundController): ServiceController, Logging {
    companion object {
        const val SERVICE_NAME = "Bisq Service"
        const val DISMISS_NOTIFICATION_ACTION = "DISMISS_NOTIFICATION"
        const val DISMISS_PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private val context = appForegroundController.context


    private val serviceScope = CoroutineScope(SupervisorJob())
    private val observerJobs = mutableMapOf<StateFlow<*>, Job>()
    private var isRunning = false

    var activityClassForIntents = context::class.java
    var defaultDestination = "tab_my_trades" // TODO minor refactor move this hardcode out of here and into client leaf code    }

    /**
     * Starts the service in the appropiate mode based on the current device running Android API
     */
    actual override fun startService() {
        if (isRunning) {
            log.w { "Service already running, skipping start call" }
        } else {
            log.i { "Starting Bisq Service.."}
            createNotificationChannel()
            val intent = Intent(context, BisqForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log.i { "OS supports foreground service" }
                context.startForegroundService(intent)
            } else {
                // if the phone does not support foreground service
                context.startService(intent)
            }
            isRunning = true
            log.i { "Started Bisq Service"}
        }
    }

    // TODO provide an access for this
    actual override fun stopService() {
        // TODO we need to leave the service running if the user is ok with it
        if (isRunning) {
            val intent = Intent(context, BisqForegroundService::class.java)
            context.stopService(intent)
            deleteNotificationChannel()
            isRunning = false
        } else {
            log.w { "Service is not running, skipping stop call" }
        }
    }

    actual override fun <T> registerObserver(stateFlow: StateFlow<T>, onStateChange: (T) -> Unit) {
        if (observerJobs.contains(stateFlow)) {
            log.w { "State flow observer already registered, skipping registration" }
        }
        val job = serviceScope.launch(Dispatchers.Default) {
            stateFlow.collect { onStateChange(it) }
        }
        observerJobs[stateFlow] = job
    }

    actual override fun unregisterObserver(stateFlow: StateFlow<*>) {
        observerJobs[stateFlow]?.cancel()
        observerJobs.remove(stateFlow)
    }

    // TODO support for on click and decide if we block on foreground
    actual fun pushNotification(title: String, message: String) {
        if (isAppInForeground()) {
            log.w { "Skipping notification since app is in the foreground" }
            return
        }

        // Create an intent that brings the user back to the app
        val intent = Intent(context, activityClassForIntents).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("destination", defaultDestination) // Add extras to navigate to a specific screen
        }

        // Create a PendingIntent to handle the notification click
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires explicit opt-in for background activity launches
            val activityOptions = ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }
            PendingIntent.getActivity(
                context,
                0,
                intent,
                pendingIntentFlags,
                activityOptions.toBundle()
            )
        } else {
            PendingIntent.getActivity(
                context,
                0,
                intent,
                pendingIntentFlags
            )
        }

        // Add dismiss action for better UX and notification management (API35+ target)
        val dismissIntent = Intent(context, BisqForegroundService::class.java).apply {
            action = DISMISS_NOTIFICATION_ACTION
        }
        val dismissPendingIntent = PendingIntent.getService(
            context,
            1,
            dismissIntent,
            DISMISS_PENDING_INTENT_FLAGS
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, BisqForegroundService.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // For android previous to O
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()
        notificationManager.notify(BisqForegroundService.PUSH_NOTIFICATION_ID, notification)
        log.d {"Pushed notification: $title: $message" }
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
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.deleteNotificationChannel(BisqForegroundService.CHANNEL_ID)
            } catch (e: Exception) {
                log.e(e) { "Failed to delete bisq notification channel" }
            }
        }
    }

    actual fun isAppInForeground(): Boolean {
        return appForegroundController.isForeground.value
    }

}