package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeReadStateMap(
    // tradeId to read count
    val map: Map<String, Int> = emptyMap()
)