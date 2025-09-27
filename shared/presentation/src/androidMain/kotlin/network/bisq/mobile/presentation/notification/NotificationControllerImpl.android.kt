package network.bisq.mobile.presentation.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import network.bisq.mobile.domain.helper.ResourceUtils
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.utils.Logging
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
        config.body?.let { builder.setContentText(it) }
        config.badgeCount?.let { builder.setNumber(it) }
        builder.setSound(
            // for older platforms
            ResourceUtils.getSoundUri(
                context,
                config.android.sound
            )
        )
        builder.setOngoing(config.android.ongoing)
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

        config.android.actions?.let { actionsList ->
            if (actionsList.isNotEmpty()) {
                actionsList.forEach { action ->
                    val pressAction = action.pressAction
                    // for now only Route is supported, we will add broadcast handlers and
                    // different types of action when necessary in a cross platform way
                    val pendingIntent = if (pressAction is NotificationPressAction.Route) {
                        createNavDeepLinkPendingIntent(pressAction.route)
                    } else {
                        null
                    }
                    pendingIntent?.let {
                        builder.addAction(NotificationCompat.Action(null, action.title, it))
                    }
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
