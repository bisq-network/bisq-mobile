package network.bisq.mobile.domain.model.trade

import kotlinx.serialization.Serializable

@Serializable
enum class TradeOutcomeFilter(
    val labelKey: String,
) {
    ALL("mobile.tradeHistory.filter.outcome.all"),
    COMPLETED("mobile.tradeHistory.filter.outcome.completed"),
    CANCELLED("mobile.tradeHistory.filter.outcome.cancelled"),
    FAILED("mobile.tradeHistory.filter.outcome.failed"),
    ;

    fun matches(outcome: TradeOutcome): Boolean =
        when (this) {
            ALL -> true
            COMPLETED -> outcome == TradeOutcome.COMPLETED
            CANCELLED -> outcome == TradeOutcome.CANCELLED || outcome == TradeOutcome.REJECTED
            FAILED -> outcome == TradeOutcome.FAILED
        }
}
