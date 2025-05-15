package network.bisq.mobile.android.node.service.reputation

import bisq.common.observable.Pin
import bisq.user.reputation.ReputationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade

class NodeReputationServiceFacade(private val applicationService: AndroidApplicationService.Provider) : ServiceFacade(),
    ReputationServiceFacade {
    private val reputationService: ReputationService by lazy { applicationService.reputationService.get() }

    // Properties
    private val _reputationByUserProfileId: MutableStateFlow<Map<String, ReputationScoreVO>> =
        MutableStateFlow(emptyMap())
    override val reputationByUserProfileId: StateFlow<Map<String, ReputationScoreVO>> get() = _reputationByUserProfileId

    // Misc
    private var reputationChangePin: Pin? = null

    // Life cycle
    override fun activate() {
        super<ServiceFacade>.activate()
        serviceScope.launch {
            observeReputation()
        }
    }

    override fun deactivate() {
        reputationChangePin?.unbind()
        reputationChangePin = null
        super<ServiceFacade>.deactivate()
    }

    // API
    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        val reputation = reputationByUserProfileId.value[userProfileId]
        return if (reputation == null) {
            Result.failure(NoSuchElementException("Reputation for userId=$userProfileId not cached yet"))
        } else {
            Result.success(reputation)
        }
    }

    // Private
    private suspend fun observeReputation() {
        reputationChangePin?.unbind()
        reputationChangePin = reputationService.userProfileIdWithScoreChange.addObserver { userProfileId ->
            try {
                if (userProfileId != null) {
                    updateUserReputation(userProfileId)
                }
            } catch (e: Exception) {
                log.e("Failed to update user reputation", e)
            }
        }
    }

    private fun updateUserReputation(userProfileId: String) {
        val reputation = reputationService.getReputationScore(userProfileId).let {
            Mappings.ReputationScoreMapping.fromBisq2Model(it)
        }

        _reputationByUserProfileId.update { current ->
            current + (userProfileId to reputation)
        }
    }
}