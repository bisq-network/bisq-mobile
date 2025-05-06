package network.bisq.mobile.android.node.service.reputation

import android.content.res.Resources.NotFoundException
import bisq.common.observable.Pin
import bisq.user.reputation.ReputationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade

class NodeReputationServiceFacade(private val applicationService: AndroidApplicationService.Provider) : ServiceFacade(),
    ReputationServiceFacade {
    private val reputationService: ReputationService by lazy { applicationService.reputationService.get() }

    // Properties
    private val _reputationByUserProfileId: MutableStateFlow<Map<String, ReputationScoreVO>> = MutableStateFlow(emptyMap())
    override val reputationByUserProfileId: StateFlow<Map<String, ReputationScoreVO>> get() = _reputationByUserProfileId

    // Misc
    private var selectedReputationPin: Pin? = null

    // Life cycle
    override fun activate() {
        super<ServiceFacade>.activate()
        observeReputation()
    }

    override fun deactivate() {
        selectedReputationPin?.unbind()
        selectedReputationPin = null
        super<ServiceFacade>.deactivate()
    }

    // API
    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        val reputation = reputationByUserProfileId.value[userProfileId]
        if (reputation == null) {
            return Result.failure(NotFoundException())
        } else {
            return Result.success(reputation)
        }
    }

    // Private
    private fun observeReputation() {
        selectedReputationPin = reputationService.userProfileIdWithScoreChange.addObserver{ userProfileId ->
            try {
                updateUserReputation(userProfileId)
            } catch (e: Exception) {
                log.e("Failed to update user reputation", e)
            }
        }
    }

    private fun updateUserReputation(userProfileId: String) {
        val reputation = reputationService.getReputationScore(userProfileId).let {
            Mappings.ReputationScoreMapping.fromBisq2Model(it)
        }
        val profileScoreMap = reputationByUserProfileId.value.toMutableMap()
        profileScoreMap[userProfileId] = reputation

        _reputationByUserProfileId.value = profileScoreMap
    }
}