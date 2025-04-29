package network.bisq.mobile.domain.data.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Trade model representing a trade in the system.
 */
@Serializable
class Trade(var tradeId: String = "") : BaseModel() {
    var makerNetworkId: String = ""
    var takerNetworkId: String = ""
    var offerAmount: Double = 0.0
    var offerCurrency: String = ""
    var price: Double = 0.0
    var status: String = ""
    var createdAt: Long = Clock.System.now().toEpochMilliseconds()
    var updatedAt: Long = Clock.System.now().toEpochMilliseconds()
    
    init {
        require(tradeId.isNotBlank()) { "Trade must have a non-blank tradeId" }
        id = tradeId
    }
}