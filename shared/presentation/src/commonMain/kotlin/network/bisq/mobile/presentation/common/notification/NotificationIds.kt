package network.bisq.mobile.presentation.common.notification

/**
 * keep all notification id definitions here for clarity
 */
object NotificationIds {
    private const val BITCOIN_PAYMENT_UPDATED_PREFIX = "bit-pay-"
    private const val PAYMENT_UPDATED_PREFIX = "acc-pay-"
    private const val NEW_CHAT_MESSAGE_PREFIX = "new-msg-"
    private const val NEW_PRIVATE_CHAT_MESSAGE_PREFIX = "pm-"
    private const val TRADE_STATE_UPDATED_PREFIX = "trade-state-"

    fun getBitcoinPaymentUpdatedId(shortTradeId: String) = BITCOIN_PAYMENT_UPDATED_PREFIX + shortTradeId

    fun getPaymentUpdatedId(shortTradeId: String) = PAYMENT_UPDATED_PREFIX + shortTradeId

    fun getNewChatMessageId(shortTradeId: String) = NEW_CHAT_MESSAGE_PREFIX + shortTradeId

    /**
     * Privacy: a two-party channel id is `domain.<profileIdA>-<profileIdB>`, so it names both ends
     * of a private conversation — strictly more than a bare profile id. Notification ids reach log
     * sinks on both platforms ([NotificationController] implementations log the id they post and
     * cancel), and logs persist to disk and ride along in bug reports, so only a digest of the
     * channel id leaves this function. The trade ids above are already shortened for the same
     * reason.
     *
     * Stability within a process is what matters here, not a value that agrees across platforms:
     * the id is produced, posted and cancelled by the same app, and iOS' NSE deliberately keys its
     * pre-rendered notifications differently (see `NotificationService.swift`). A hash collision
     * would merely make two conversations share one tray entry; Android already reduces the id to
     * `hashCode()` before handing it to the system.
     */
    fun getNewPrivateChatMessageId(channelId: String) = NEW_PRIVATE_CHAT_MESSAGE_PREFIX + channelId.hashCode().toUInt().toString(16)

    fun getTradeStateUpdatedId(shortTradeId: String) = TRADE_STATE_UPDATED_PREFIX + shortTradeId
}
