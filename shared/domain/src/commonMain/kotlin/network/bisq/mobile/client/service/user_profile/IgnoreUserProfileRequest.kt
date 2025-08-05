package network.bisq.mobile.client.service.user_profile

import kotlinx.serialization.Serializable

@Serializable
data class IgnoreUserProfileRequest(
    val profileId: String,
)