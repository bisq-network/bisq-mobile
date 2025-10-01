package network.bisq.mobile.presentation.notification.model

data class IosNotificationCategory(
    val id: String,
    /**
     * Only for defining the button and the type of the action
     *
     * action details (like which route to navigate to) will not be used here and
     * needs to be handled on iosClient using userInfo
     */
    val actions: List<NotificationButton>,
)