package network.bisq.mobile.client.common.domain.service.user_profile

import kotlinx.serialization.Serializable

@Serializable
data class SelectUserProfileRequest(
    val userProfileId: String,
)
