package network.bisq.mobile.client.service.trades

import kotlinx.serialization.Serializable

@Serializable
data class TakeOfferRequest(
    val offerId: String,
    val baseSideAmount: Long,
    val quoteSideAmount: Long,
    val bitcoinPaymentMethod: String,
    val fiatPaymentMethod: String
)