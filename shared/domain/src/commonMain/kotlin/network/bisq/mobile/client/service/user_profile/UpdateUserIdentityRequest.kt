package network.bisq.mobile.client.service.user_profile

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserIdentityRequest(
    val terms: String = "",
    val statement: String = ""
)