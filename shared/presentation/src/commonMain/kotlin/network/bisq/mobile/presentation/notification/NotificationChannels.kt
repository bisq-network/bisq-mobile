package network.bisq.mobile.presentation.notification

/**
 * keep all channel id definitions here for clarity.
 *
 * We also use the same IDs for iOS categories.
 *
 * For every notification type a new Channel or Category must be defined, as actions are defined on
 * categories on iOS.
 */
object NotificationChannels {
    const val BISQ_SERVICE = "BISQ_SERVICE_CHANNEL"
    const val TRADE_UPDATES = "BISQ_TRADE_UPDATES_CHANNEL"
    const val USER_MESSAGES = "USER_MESSAGES_CHANNEL"
}