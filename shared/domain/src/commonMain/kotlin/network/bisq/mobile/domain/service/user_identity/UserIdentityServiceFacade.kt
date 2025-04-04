package network.bisq.mobile.domain.service.user_identity

import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO

interface UserIdentityServiceFacade {
    fun findUserIdentity(id: String): UserIdentityVO?
}