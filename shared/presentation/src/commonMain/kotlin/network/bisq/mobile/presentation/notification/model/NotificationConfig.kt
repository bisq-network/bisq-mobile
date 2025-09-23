package network.bisq.mobile.presentation.notification.model

// extend the configuration api and implement each platform as needed
/**
 * Configuration for displaying a notification
 */
data class NotificationConfig(
    val id: String,
    val skipInForeground: Boolean,
    val title: String? = null,
    val subtitle: String? = null,
    val body: String? = null,
    /** sets the number that your app’s badge displays on a best effort basis */
    val badgeCount: Int? = null,
    val android: AndroidNotificationConfig? = null,
    val ios: IosNotificationConfig? = null,
) {

    init {
        if (id.isBlank()) {
            throw IllegalArgumentException("notification id cannot be blank")
        }
        if (badgeCount != null && badgeCount < 0) {
            throw IllegalArgumentException("badgeCount cannot be less than 0")
        }
        val criticalVolume = ios?.criticalVolume
        if (criticalVolume != null && (criticalVolume < 0 || criticalVolume > 1)) {
            throw IllegalArgumentException("criticalVolume must be between 0 and 1")
        }
    }
}
