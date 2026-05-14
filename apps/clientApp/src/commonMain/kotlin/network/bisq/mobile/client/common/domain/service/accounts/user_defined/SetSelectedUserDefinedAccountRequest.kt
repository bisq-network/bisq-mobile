package network.bisq.mobile.client.common.domain.service.accounts.user_defined

import kotlinx.serialization.Serializable

@Serializable
data class SetSelectedUserDefinedAccountRequest(
    val accountName: String,
)
