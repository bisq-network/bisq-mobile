package network.bisq.mobile.domain.model.trade

import kotlinx.serialization.Serializable

@Serializable
enum class TradeSort(
    val labelKey: String,
) {
    NEWEST_FIRST("mobile.tradeHistory.sort.newestFirst"),
    OLDEST_FIRST("mobile.tradeHistory.sort.oldestFirst"),
    AMOUNT_HIGH_LOW("mobile.tradeHistory.sort.amountHighLow"),
    AMOUNT_LOW_HIGH("mobile.tradeHistory.sort.amountLowHigh"),
}
