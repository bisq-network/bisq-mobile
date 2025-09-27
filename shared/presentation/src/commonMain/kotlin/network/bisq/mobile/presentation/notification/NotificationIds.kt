package network.bisq.mobile.presentation.notification

/**
 * keep all notification id definitions here for clarity
 */
object NotificationIds {
    private const val BITCOIN_PAYMENT_UPDATED_PREFIX = "bit-pay-"
    private const val PAYMENT_UPDATED_PREFIX = "acc-pay-"
    private const val NEW_CHAT_MESSAGE_PREFIX = "new-msg-"
    private const val TRADE_STATE_UPDATED_PREFIX = "trade-state-"

    fun getBitcoinPaymentUpdatedId(shortTradeId: String) =
        BITCOIN_PAYMENT_UPDATED_PREFIX + shortTradeId

    fun getPaymentUpdatedId(shortTradeId: String) =
        PAYMENT_UPDATED_PREFIX + shortTradeId

    fun getNewChatMessageId(shortTradeId: String) =
        NEW_CHAT_MESSAGE_PREFIX + shortTradeId

    fun getTradeStateUpdatedId(shortTradeId: String) =
        TRADE_STATE_UPDATED_PREFIX + shortTradeId
}