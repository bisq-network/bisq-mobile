package network.bisq.mobile.client.service.offers

import kotlinx.serialization.Serializable

@Serializable
data class AddAccountResponse(val accountName: String)