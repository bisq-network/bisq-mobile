package network.bisq.mobile.client.service.offers

import kotlinx.serialization.Serializable

@Serializable
data class CreateOfferResponse(val offerId: String)