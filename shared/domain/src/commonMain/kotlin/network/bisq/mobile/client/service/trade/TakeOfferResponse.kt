package network.bisq.mobile.client.service.trade

import kotlinx.serialization.Serializable

@Serializable
data class TakeOfferResponse(val tradeId: String)