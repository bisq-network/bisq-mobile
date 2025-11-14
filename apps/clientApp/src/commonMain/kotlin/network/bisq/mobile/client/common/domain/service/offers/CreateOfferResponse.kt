package network.bisq.mobile.client.common.domain.service.offers

import kotlinx.serialization.Serializable

@Serializable
data class CreateOfferResponse(val offerId: String)