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
    /**
     * Overrides the sound the notification is displayed with. To play the default system sound use "default" (used if not set). `null` means no sound.
     *
     * **Android:** Starting with Android 8.0 (API level 26), notification sounds are managed by
     * NotificationChannels. Therefore, this method is a no-op on
     * API level 26 and higher. You should configure the sound directly on the channel
     * for these versions.
     *
     * **iOS:** The name of the sound file to be played. The sound must be in the Library/Sounds folder of the app's data container or the Library/Sounds folder of an app group data container.
     */
    val sound: String? = "default",
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
