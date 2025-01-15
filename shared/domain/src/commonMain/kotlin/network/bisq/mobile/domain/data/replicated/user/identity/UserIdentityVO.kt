package network.bisq.mobile.domain.data.replicated.user.identity

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.identity.IdentityVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

@Serializable
data class UserIdentityVO(val identity: IdentityVO, val userProfile: UserProfileVO)