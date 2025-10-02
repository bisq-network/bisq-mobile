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


    /**
     * the press action for when user presses the notification itself. by default it will open the app on press. setting this to `null` has no effect on iOS.
     */
    var pressAction: NotificationPressAction = NotificationPressAction.Default()

    /**
     * categoryId must be provided to show the correct actions on iOS
     */
    var actions: List<NotificationButton>? = null

    /**
     * actions must be provided to set correct userInfo for category
     */
    var categoryId: String? = null
}
