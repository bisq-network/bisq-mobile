package network.bisq.mobile.client.common.domain.service.trades

import kotlinx.serialization.Serializable

@Serializable
data class TakeOfferResponse(
    val tradeId: String,
)
