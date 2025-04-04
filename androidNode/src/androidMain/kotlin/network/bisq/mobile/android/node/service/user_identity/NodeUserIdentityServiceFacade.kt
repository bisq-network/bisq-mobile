package network.bisq.mobile.android.node.service.user_identity

import bisq.identity.IdentityService
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import bisq.user.reputation.ReputationService
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_identity.UserIdentityServiceFacade
import network.bisq.mobile.domain.utils.Logging

class NodeUserIdentityServiceFacade(private val applicationService: AndroidApplicationService.Provider) :
        UserIdentityServiceFacade, Logging {
            private val identityService: UserIdentityService by lazy {
                applicationService.userIdentityService.get()
            }

    override fun findUserIdentity(id: String): UserIdentityVO? {
        val userIdentity = identityService.findUserIdentity(id);
        if (userIdentity.isPresent) {
            return Mappings.UserIdentityMapping.fromBisq2Model(userIdentity.get())
        } else {
            return null
        }
    }

}
