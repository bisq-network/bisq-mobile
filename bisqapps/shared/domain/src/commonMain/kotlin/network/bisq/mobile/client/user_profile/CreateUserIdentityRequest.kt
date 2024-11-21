package network.bisq.mobile.domain.client.main.user_profile

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.replicated_model.user.identity.PreparedData

@Serializable
data class CreateUserIdentityRequest(
    val nickName: String,
    val terms: String = "",
    val statement: String = "",
    val preparedData: PreparedData
)