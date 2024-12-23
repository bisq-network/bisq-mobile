package network.bisq.mobile.client.service.offer

import kotlinx.serialization.Serializable

@Serializable
data class CreateOfferResponse(val offerId: String)