package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeReadState(
    val tradeId: String = "",
    val readCount: Int = 0
): BaseModel() {
    init {
        require(tradeId.isNotBlank()) { "TradeReadState must have a non-blank tradeId" }
        id = tradeId
    }
}