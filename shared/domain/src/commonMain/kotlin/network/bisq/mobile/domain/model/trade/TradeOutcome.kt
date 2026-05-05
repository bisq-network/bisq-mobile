package network.bisq.mobile.domain.model.trade

import kotlinx.serialization.Serializable

@Serializable
enum class TradeOutcome {
    COMPLETED,
    CANCELLED,
    REJECTED,
    FAILED,
}
