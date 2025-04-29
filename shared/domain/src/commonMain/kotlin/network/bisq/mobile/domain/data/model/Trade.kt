package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

/**
 * Trade model representing a trade in the system.
 */
@Serializable
class Trade : BaseModel() {
    var tradeId: String = ""
    var makerNetworkId: String = ""
    var takerNetworkId: String = ""
    var offerAmount: Double = 0.0
    var offerCurrency: String = ""
    var price: Double = 0.0
    var status: String = ""
    var createdAt: Long = 0
    var updatedAt: Long = 0
    
    init {
        // Use tradeId as the BaseModel id
        id = tradeId
    }
    
    // Update id when tradeId changes
    fun updateTradeId(newTradeId: String) {
        tradeId = newTradeId
        id = newTradeId
    }
}