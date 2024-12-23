package network.bisq.mobile.client.service.user_profile

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserIdentityRequest(
    val nickName: String,
    val terms: String = "",
    val statement: String = "",
    val preparedData: network.bisq.mobile.domain.replicated.user.identity.PreparedData
)