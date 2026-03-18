package network.bisq.mobile.data.replicated.user.identity

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.identity.IdentityVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

@Serializable
data class UserIdentityVO(
    val identity: IdentityVO,
    val userProfile: UserProfileVO,
)
