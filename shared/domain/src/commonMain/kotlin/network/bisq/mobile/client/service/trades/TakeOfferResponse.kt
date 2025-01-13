package network.bisq.mobile.client.service.trades

import kotlinx.serialization.Serializable

@Serializable
data class TakeOfferResponse(val tradeId: String)