package network.bisq.mobile.data.model

import kotlinx.serialization.Serializable

/**
 * Why this device cancelled a trade, kept so the cancelled trade screen can say why. Local-only:
 * bisq2 has no cancel reason, so the peer and other devices paired with the same node never see it.
 * Entries are removed when the trade is closed.
 */
@Serializable
data class TradeCancelReasonMap(
    // tradeId to the reason this device cancelled it
    val map: Map<String, TradeCancelReason> = emptyMap(),
)

@Serializable
enum class TradeCancelReason {
    BANNED_ACCOUNT_DATA,
}
