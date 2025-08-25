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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TradeReadState) return false
        return tradeId == other.tradeId && readCount == other.readCount
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + readCount
        result = 31 * result + tradeId.hashCode()
        return result
    }
}