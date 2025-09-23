package network.bisq.mobile.presentation.notification.model

/**
 * Represents a notification button that will do an action on press
 */
data class NotificationButton(
    val title: String,
    val pressAction: NotificationPressAction,
)
