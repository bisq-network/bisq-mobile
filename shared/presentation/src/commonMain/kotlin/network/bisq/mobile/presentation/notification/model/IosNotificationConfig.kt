package network.bisq.mobile.presentation.notification.model

/**
 * iOS-specific notification configuration
 */
class IosNotificationConfig {
    /** use "default" for default sound. `null` means no sound. The name of the sound file to be played. The sound must be in the Library/Sounds folder of the app's data container or the Library/Sounds folder of an app group data container. */
    var sound: String? = "default"

    /** The notification’s importance and required delivery timing. */
    var interruptionLevel: IosNotificationInterruptionLevel? = null

    /**
     * critical alerts will bypass the mute switch and Do Not Disturb.
     */
    var critical: Boolean = false

    /**
     * The volume must be a value between 0.0 and 1.0.
     */
    var criticalVolume: Float? = null

    var actions: List<NotificationButton>? = null
}
