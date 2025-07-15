package network.bisq.mobile.client.service.user_profile

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

// user for updates responses too
@Serializable
data class CreateUserIdentityResponse(val userProfile: UserProfileVO)