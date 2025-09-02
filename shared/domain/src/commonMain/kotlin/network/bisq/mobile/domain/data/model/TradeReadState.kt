package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeReadState(
    val tradeId: String = "",
    val readCount: Int = 0
) {
    init {
        require(tradeId.isNotBlank()) { "TradeReadState must have a non-blank tradeId" }
        require(readCount >= 0) { "TradeReadState.readCount must be >= 0" }
    }
}