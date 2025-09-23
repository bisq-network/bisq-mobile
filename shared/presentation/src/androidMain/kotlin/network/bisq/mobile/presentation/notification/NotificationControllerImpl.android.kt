package network.bisq.mobile.presentation.notification

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
import androidx.core.net.toUri
import network.bisq.mobile.domain.helper.ResourceUtils
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.notification.model.NotificationConfig
import network.bisq.mobile.presentation.notification.model.NotificationPressAction
import network.bisq.mobile.presentation.notification.model.toNotificationCompat
import network.bisq.mobile.presentation.ui.navigation.Routes

class NotificationControllerImpl(
    private val appForegroundController: AppForegroundController,
    val activityClassForIntents: Class<*>,
) : NotificationController, Logging {
    companion object {
        // we use this to avoid linter errors, as it's handled internally by
        // ContextCompat.checkSelfPermission for different android versions
        const val POST_NOTIFS_PERM = "android.permission.POST_NOTIFICATIONS"
    }

    private val context get() = appForegroundController.context


    override fun doPlatformSpecificSetup() {
        createNotificationChannels()
    }

    override suspend fun hasPermission(): Boolean {
        return hasPermissionSync()
    }

    private fun hasPermissionSync(): Boolean {
        // ContextCompat.checkSelfPermission handles notifications permission for different versions
        return ContextCompat.checkSelfPermission(
            context,
            POST_NOTIFS_PERM,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override // incorrect lint, as we already check for permission
    fun notify(config: NotificationConfig) {
        log.i { "android pushNotification called - title: '${config.title}', body: '${config.body}', isAppInForeground: ${isAppInForeground()}" }

        if (config.skipInForeground && isAppInForeground()) {
            log.w { "Skipping notification since app is in the foreground and skipInForeground is true" }
            return
        }

        if (!hasPermissionSync()) {
            log.e { "POST_NOTIFICATIONS permission not granted, cannot send notification" }
            return
        }

        val notificationId = config.id.hashCode()

        val channelId = config.android?.channelId
            ?: throw IllegalArgumentException("android notification config should define channelId")

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(ResourceUtils.getNotifResId(context))
            .setDefaults(NotificationCompat.DEFAULT_ALL) // for older platforms

        config.title?.let { builder.setContentTitle(it) }
        config.subtitle?.let { builder.setSubText(it) }
        config.badgeCount?.let { builder.setNumber(it) }
        builder.setSound(
            // for older platforms
            ResourceUtils.getSoundUri(
                context,
                config.android.sound
            )
        )
        builder.setOngoing(config.android.ongoing)
        // Defaults priority to DEFAULT to avoid OS killing the app
        builder.setPriority(config.android.priority.toNotificationCompat())
        builder.setCategory(config.android.category.toNotificationCompat())
        builder.setOnlyAlertOnce(config.android.onlyAlertOnce)
        builder.setShowWhen(config.android.showTimestamp)
        config.android.timestamp?.let { builder.setWhen(it) }
        builder.setGroup(config.android.group)
        builder.setVisibility(config.android.visibility.toNotificationCompat())
        builder.setAutoCancel(config.android.autoCancel)
        config.android.sortKey?.let { builder.setSortKey(it) }

        if (config.android.pressAction == null) {
            builder.setContentIntent(null)
        } else {
            val action = config.android.pressAction
            when (action) {
                is NotificationPressAction.Route -> builder.setContentIntent(
                    createNavDeepLinkPendingIntent(action.route)
                )

                is NotificationPressAction.Default ->  builder.setContentIntent(
                    createNavDeepLinkPendingIntent(Routes.TabOpenTradeList)
                )

                null -> {
                    builder.setContentIntent(null)
                }
            }
        }

        config.android.actions?.let {
            if (it.isNotEmpty()) {
                it.forEach { action ->
                    val pressAction = action.pressAction
                    // for now only Route is supported, we will add broadcast handlers and
                    // different types of action when necessary in a cross platform way
                    val pendingIntent = if (pressAction is NotificationPressAction.Route) {
                        createNavDeepLinkPendingIntent(pressAction.route)
                    } else {
                        null
                    }
                    builder.addAction(NotificationCompat.Action(null, action.title, pendingIntent))
                }
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            log.e(e) { "Failed to push notification with ID $notificationId" }
        }
    }

    override fun cancel(id: String) {
        NotificationManagerCompat.from(context).cancel(id.hashCode())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                AndroidNotificationChannels.BISQ_SERVICE_CHANNEL_ID,
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
                AndroidNotificationChannels.TRADE_AND_UPDATES_CHANNEL_ID,
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

            val manager =
                NotificationManagerCompat.from(context)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(tradeAndUpdatesChannel)
            log.i { "Created notification channel with IMPORTANCE_HIGH" }
        }
    }

    override fun isAppInForeground(): Boolean {
        return appForegroundController.isForeground.value
    }

    // TODO: fix after nav refactor to accept a Route class
    fun createNavDeepLinkPendingIntent(route: Routes): PendingIntent {
        val link = Routes.getDeeplinkUriString(route)
        val intent = Intent(
            Intent.ACTION_VIEW,
            link.toUri(),
            context,
            activityClassForIntents
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getActivity(
            context,
            link.hashCode(),
            intent,
            pendingIntentFlags
        )

        return pendingIntent
    }
}