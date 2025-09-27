package network.bisq.mobile.presentation.notification.model

/**
 * Android-specific notification configuration
 */
class AndroidNotificationConfig {
    /**
     * Specifies the `AndroidChannel` which the notification will be delivered on.
     *
     * On Android 8.0 (API 26) the channel ID is required. Providing a invalid channel ID will throw
     * an error.
     */
    var channelId: String? = null

    /**
     * `null` means no sound. use "default" to use default sound.
     *
     * Starting with Android 8.0 (API level 26), notification sounds are managed by
     * NotificationChannels. Therefore, this method is a no-op on
     * API level 26 and higher. You should configure the sound directly on the channel
     * for these versions.
     */
    var sound: String? = "default"

    /**
     * whether this is an "ongoing" notification.
     *
     * Ongoing notifications cannot be dismissed by the user on locked devices, or by notification listeners, and some notifications (call, device management, media) cannot be dismissed on unlocked devices, so your application or service must take care of canceling them.
     *
     * They are typically used to indicate a background task that the user is actively engaged with (e.g., playing music) or is pending in some way and therefore occupying the device (e.g., a file download, sync operation, active network connection).
     */
    var ongoing: Boolean = false

    /**
     * Set this flag if you would only like the sound, vibrate and ticker to be played if the notification is not already showing.
     *
     * Note that using this flag will stop any ongoing alerting behaviour such as sound, vibration or blinking notification LED.
     */
    var onlyAlertOnce: Boolean = false

    /**
     * the priority of this notification. defaults to PRIORITY_DEFAULT
     */
    var priority: AndroidNotificationPriority? = null

    var category: AndroidNotificationCategory? = null


    /**
     * `setShowWhen` is called with this value
     */
    var showTimestamp: Boolean = true

    /**
     * Set the time that the event occurred. Notifications in the panel are sorted by this time. `setWhen` is called with this value.
     */
    var timestamp: Long? = null

    /**
     * Set this notification to be part of a group of notifications sharing the same key. Grouped notifications may display in a cluster or stack on devices which support such rendering.
     */
    var group: String? = null

    /**
     * Sphere of visibility of this notification, which affects how and when the SystemUI reveals the notification's presence and contents in untrusted situations (namely, on the secure lockscreen and during screen sharing).
     *
     * @see <a href="https://developer.android.com/reference/android/app/Notification#visibility">Notification#visibility</a>
     */
    var visibility: AndroidNotificationVisibility = AndroidNotificationVisibility.VISIBILITY_PUBLIC

    /**
     * Make this notification automatically dismissed when the user touches it.
     */
    var autoCancel: Boolean = true

    /**
     * Notifications will be sorted lexicographically using this value, although providing different priorities in addition to providing sort key may cause this value to be ignored.
     *
     * This sort key can also be used to order members of a notification group.
     */
    var sortKey: String? = null

    /**
     * the press action for when user presses the notification itself. by default it will open the app on press. setting this to `null` will cause it to do nothing
     */
    var pressAction: NotificationPressAction? = NotificationPressAction.Default()

    /**
     * extra actions that are shown under notification
     */
    var actions: List<NotificationButton>? = null
}