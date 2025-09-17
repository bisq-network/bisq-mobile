package network.bisq.mobile.domain.service.notifications.controller

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.helper.ResourceUtils
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.BisqForegroundService
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n


/**
 * Controller interacting with the bisq service
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class NotificationServiceController(private val appForegroundController: AppForegroundController, val activityClassForIntents: Class<*>): ServiceController, Logging {
    companion object {
        const val DISMISS_NOTIFICATION_ACTION = "DISMISS_NOTIFICATION"
        const val DISMISS_PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        // NOTE: to avoid referencing dep Routes.TabOpenTradeList.name
        const val MY_TRADES_TAB = "tab_my_trades"
        const val EXTRA_DESTINATION = "destination"

        // we use this to avoid linter errors, as it's handled internally by
        // ContextCompat.checkSelfPermission for different android versions
        const val POST_NOTIFS_PERM = "android.permission.POST_NOTIFICATIONS"

        const val TRADE_AND_UPDATES_CHANNEL_ID = "BISQ_TRADE_AND_UPDATES_CHANNEL"
    }

    private val context get() = appForegroundController.context

    private val serviceScope = CoroutineScope(SupervisorJob())
    private val observerJobs = mutableMapOf<StateFlow<*>, Job>()
    private var isRunning = false
    private val defaultDestination = MY_TRADES_TAB

    actual fun doPlatformSpecificSetup() {
        createNotificationChannels()
    }

    actual suspend fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            POST_NOTIFS_PERM
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts the service in the appropiate mode based on the current device running Android API
     */
    actual override fun startService() {
        if (isRunning) {
            log.w { "Service already running, skipping start call" }
        } else {
            log.i { "Starting Bisq Service.." }
            createNotificationChannels()
            val intent = Intent(context, BisqForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log.i { "OS supports foreground service" }
                context.startForegroundService(intent)
            } else {
                // if the phone does not support foreground service
                context.startService(intent)
            }
            isRunning = true
            log.i { "Started Bisq Service" }
        }
    }

    actual override fun stopService() {
        // TODO if we ever implement Live notifications even if app was killed
        //  we need to leave the service running if the user is ok with it
        if (isRunning) {
            log.i { "Stopping BisqForegroundService" }
            val intent = Intent(context, BisqForegroundService::class.java)
            context.stopService(intent)
            isRunning = false
            log.i { "BisqForegroundService stopped" }
        } else {
            log.i { "BisqForegroundService is not running, skipping stop call" }
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

    @SuppressLint("MissingPermission")
    actual fun pushNotification(title: String, message: String) {
        log.i { "pushNotification called - title: '$title', message: '$message', isAppInForeground: ${isAppInForeground()}" }

        if (isAppInForeground()) {
            log.w { "Skipping notification since app is in the foreground" }
            return
        }

        if (
            ContextCompat.checkSelfPermission(context, POST_NOTIFS_PERM)
            != PackageManager.PERMISSION_GRANTED
        ) {
            log.e { "POST_NOTIFICATIONS permission not granted, cannot send notification" }
            return
        }

        // Generate unique notification ID to avoid overwriting previous notifications
        val notificationId = generateNotificationId(title, message)

        // Create an intent that brings the user back to the app
        val intent = Intent(context, activityClassForIntents).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION, defaultDestination) // Add extras to navigate to a specific screen
        }

        // Create a PendingIntent to handle the notification click
        // Use unique request code to avoid PendingIntent conflicts
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId, // Use unique request code
            intent,
            pendingIntentFlags
        )

        // Add dismiss action for better UX and notification management (API35+ target)
        val dismissIntent = Intent(context, BisqForegroundService::class.java).apply {
            action = DISMISS_NOTIFICATION_ACTION
            putExtra("notificationId", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getService(
            context,
            notificationId + 10000, // Offset to avoid conflicts
            dismissIntent,
            DISMISS_PENDING_INTENT_FLAGS
        )

        val notificationManager = NotificationManagerCompat.from(context)
        val notification = NotificationCompat.Builder(context, TRADE_AND_UPDATES_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(ResourceUtils.getNotifResId(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Default priority to avoid OS killing the app
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            // Enhanced visibility for Android 15+
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            // Ensure notification is not grouped or minimized
            .setOnlyAlertOnce(false)
            .setLocalOnly(false)
            // Add group for better organization on Android 15+
            .setGroup("BISQ_TRADE_NOTIFICATIONS")
            .build()

        try {
            notificationManager.notify(notificationId, notification)
            log.i { "Successfully pushed notification with ID $notificationId: $title: $message" }
        } catch (e: Exception) {
            log.e(e) { "Failed to push notification with ID $notificationId: $title: $message" }
        }
    }

    private fun generateNotificationId(title: String, message: String): Int {
        // Generate a unique ID based on content to avoid collisions
        // Start from 1000 to avoid conflicts with service notification IDs
        return (title + message).hashCode().let { hash ->
            if (hash < 1000) hash + 1000 else hash
        }.let { id ->
            // Ensure it's positive and not conflicting with service IDs
            kotlin.math.abs(id) % 100000 + 1000
        }
    }

    actual override fun isServiceRunning() = isRunning

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                BisqForegroundService.CHANNEL_ID,
                "mobile.android.channels.service".i18n(),
                NotificationManager.IMPORTANCE_DEFAULT // Default importance to avoid OS killing the app
            ).apply {
                description = "Bisq trade notifications and updates"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
                // trick to have a sound but have it silent, as it's required for IMPORTANCE_DEFAULT
                val soundUri = ResourceUtils.getSoundUri(context, "silent.mp3")
                if (soundUri != null) {
                    val audioAttributes =
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    setSound(soundUri, audioAttributes)
                } else {
                    log.w { "Unable to retrieve silent.mp3 sound uri for service channel" }
                }
            }

            val tradeAndUpdatesChannel = NotificationChannel(
                TRADE_AND_UPDATES_CHANNEL_ID,
                "mobile.android.channels.tradeAndUpdates".i18n(),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Bisq trade notifications and updates"
                enableLights(false) // Reduce aggressive behavior
                enableVibration(true)
                setShowBadge(true)
                // Keep bubbles disabled for now
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
            }

            val manager = NotificationManagerCompat.from(context)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(tradeAndUpdatesChannel)
            log.i { "Created notification channel with IMPORTANCE_HIGH" }
        }
    }

    actual fun isAppInForeground(): Boolean {
        return appForegroundController.isForeground.value
    }

}