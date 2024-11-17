package network.bisq.mobile.domain.client.main.user_profile

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserIdentityRequest(
    val nickName: String,
    val terms: String = "",
    val statement: String = "",
    val preparedDataJson: String
)