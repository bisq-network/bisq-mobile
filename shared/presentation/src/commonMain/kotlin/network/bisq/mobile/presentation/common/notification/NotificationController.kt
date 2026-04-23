package network.bisq.mobile.presentation.common.notification

import network.bisq.mobile.presentation.common.notification.model.NotificationBuilder
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig

interface NotificationController {
    suspend fun hasPermission(): Boolean

    fun notify(builder: NotificationBuilder.() -> Unit) =
        notify(
            NotificationBuilder().apply(builder).build(),
        )

    fun notify(config: NotificationConfig)

    fun cancel(id: String)

    fun isAppInForeground(): Boolean

    /**
     * Removes any delivered notifications that were posted by a platform-specific
     * pre-rendering hook (e.g. iOS Notification Service Extension). Called when the
     * app enters foreground so stale placeholder notifications don't linger.
     * Default no-op; overridden on platforms that use pre-rendering hooks.
     */
    fun clearPreRenderedNotifications() {}
}
