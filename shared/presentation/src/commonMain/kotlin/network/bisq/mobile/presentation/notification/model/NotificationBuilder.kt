package network.bisq.mobile.presentation.notification.model

import network.bisq.mobile.domain.utils.StringUtils

/**
 * DSL builder for notifications, inspired by [Notifee API](https://notifee.app/react-native/docs/usage)
 */
class NotificationBuilder {
    /**
     * A unique identifier for your notification.
     *
     * Notifications with the same ID replace each other, allowing you to update a notification.
     *
     * Defaults to a random string if not provided.
     */
    var id: String? = null
    var title: String? = null
    var subtitle: String? = null
    var body: String? = null
    var badgeCount: Int? = null
    var android: AndroidNotificationConfig? = null
    var ios: IosNotificationConfig? = null

    /** Controls whether the notification should be displayed when the app is in foreground or not */
    var skipInForeground = true

    fun android(block: AndroidNotificationConfig.() -> Unit) {
        android = AndroidNotificationConfig().apply(block)
    }

    fun ios(block: IosNotificationConfig.() -> Unit) {
        ios = IosNotificationConfig().apply(block)
    }

    fun build(): NotificationConfig {
        return NotificationConfig(
            id = id ?: StringUtils.randomAlphaNum(),
            skipInForeground = skipInForeground,
            title = title,
            subtitle = subtitle,
            body = body,
            badgeCount = badgeCount,
            android = android,
            ios = ios,
        )
    }
}
