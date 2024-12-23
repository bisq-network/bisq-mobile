package network.bisq.mobile.client.service.user_profile

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserIdentityResponse(val userProfileId: String)