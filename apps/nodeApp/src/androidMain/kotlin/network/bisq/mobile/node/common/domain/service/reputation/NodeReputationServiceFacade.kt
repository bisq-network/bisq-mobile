package network.bisq.mobile.node.common.domain.service.reputation

import bisq.common.observable.Pin
import bisq.user.reputation.ReputationScore
import bisq.user.reputation.ReputationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.utils.resultCatching
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService

class NodeReputationServiceFacade(
    private val applicationService: AndroidApplicationService.Provider,
) : ServiceFacade(),
    ReputationServiceFacade {
    private val reputationService: ReputationService by lazy { applicationService.reputationService.get() }
    private val pins: MutableList<Pin> = mutableListOf()

    private val _scoreByUserProfileId = MutableStateFlow<Map<String, Long>>(emptyMap())
    override val scoreByUserProfileId: StateFlow<Map<String, Long>> = _scoreByUserProfileId.asStateFlow()

    // Life cycle
    override suspend fun activate() {
        super<ServiceFacade>.activate()

        // Copied rather than published directly: reputationService.scoreByUserProfileId is a live
        // ObservableHashMap, so handing it out would let collectors read it mid-update and would
        // defeat the StateFlow's equality check. Bisq2 sets userProfileIdWithScoreChange right after
        // every put into that map, which is the signal that a new score landed.
        publishScores()
        pins += reputationService.userProfileIdWithScoreChange.addObserver { publishScores() }
    }

    override suspend fun deactivate() {
        pins.forEach { it.unbind() }
        pins.clear()

        super<ServiceFacade>.deactivate()
    }

    private fun publishScores() {
        _scoreByUserProfileId.value = reputationService.scoreByUserProfileId.toMap()
    }

    // API
    // No profile id or profile age in any message below, same reasoning as
    // NodePrivateChatServiceFacade.findOrCreateChannel: these are lookups of the peers this device
    // viewed, the exception message becomes the Result.failure cause that BasePresenter.handleError
    // logs verbatim, and device logs travel in bug reports. The caller already has the id it passed.
    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> =
        withContext(Dispatchers.Default) {
            resultCatching {
                val score: ReputationScore = reputationService.getReputationScore(userProfileId)
                Mappings.ReputationScoreMapping.fromBisq2Model(score)
            }.onFailure { e -> log.e(e) { "Failed to get reputation" } }
        }

    override suspend fun getProfileAge(userProfileId: String): Result<Long?> =
        withContext(Dispatchers.Default) {
            resultCatching {
                val userService = applicationService.userService.get()
                val userProfile = userService.userProfileService.findUserProfile(userProfileId)
                if (!userProfile.isPresent) {
                    throw NoSuchElementException("UserProfile not found")
                }
                val profileAge = reputationService.profileAgeService.getProfileAge(userProfile.get())

                if (profileAge.isPresent) {
                    log.d { "Profile age resolved from ProfileAgeService" }
                    profileAge.get()
                } else {
                    log.d { "No profile age data available from ProfileAgeService" }
                    null
                }
            }.onFailure { e -> log.e(e) { "Failed to get profile age" } }
        }
}
