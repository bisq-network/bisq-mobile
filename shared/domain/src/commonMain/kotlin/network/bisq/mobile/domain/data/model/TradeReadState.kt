package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeReadState(var map: Map<String, Int> = emptyMap()): BaseModel()