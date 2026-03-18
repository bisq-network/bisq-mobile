package network.bisq.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeReadStateMap(
    // tradeId to read count
    val map: Map<String, Int> = emptyMap(),
)
