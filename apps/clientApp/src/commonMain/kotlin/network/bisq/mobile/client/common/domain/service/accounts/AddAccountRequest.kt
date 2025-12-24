package network.bisq.mobile.client.common.domain.service.accounts

import kotlinx.serialization.Serializable

@Serializable
data class AddAccountRequest(
    val accountName: String,
    val accountData: String,
)
