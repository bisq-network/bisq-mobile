package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
open class TradeReadState: BaseModel() {
    // tradeId to messageCount
    open var map: Map<String, Int> = emptyMap()
}