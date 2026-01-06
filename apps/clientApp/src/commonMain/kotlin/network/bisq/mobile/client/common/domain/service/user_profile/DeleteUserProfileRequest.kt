package network.bisq.mobile.client.common.domain.service.user_profile

import kotlinx.serialization.Serializable

@Serializable
data class DeleteUserProfileRequest(
    val userProfileId: String,
)
