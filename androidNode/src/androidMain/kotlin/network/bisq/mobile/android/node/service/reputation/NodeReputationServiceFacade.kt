package network.bisq.mobile.android.node.service.reputation

import bisq.common.application.DevMode
import bisq.common.observable.Pin
import bisq.user.reputation.ReputationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    override val reputationByUserProfileId: StateFlow<Map<String, ReputationScoreVO>> get() = _reputationByUserProfileId.asStateFlow()

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
        val score = when {
            useDevModeReputationScore() -> provideDevModeReputationScore(userProfileId)
            else -> reputationByUserProfileId.value[userProfileId]
        }
        return score?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Reputation for userId=$userProfileId not found"))
    }

    override suspend fun getProfileAge(userProfileId: String): Result<Long?> {
        return try {
            val userService = applicationService.userService.get()
            val userProfile = userService.userProfileService.findUserProfile(userProfileId)

            if (userProfile.isPresent) {
                val profile = userProfile.get()
                val profileAge = reputationService.profileAgeService.getProfileAge(profile)

                if (profileAge.isPresent) {
                    log.d { "Profile age from ProfileAgeService: ${profileAge.get()} for userId=$userProfileId" }
                    Result.success(profileAge.get())
                } else {
                    log.d { "No profile age data available from ProfileAgeService for userId=$userProfileId" }
                    Result.success(null)
                }
            } else {
                Result.failure(NoSuchElementException("UserProfile for userId=$userProfileId not found"))
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to get profile age for userId=$userProfileId" }
            Result.failure(e)
        }
    }

    private fun useDevModeReputationScore() = DevMode.isDevMode() && DevMode.devModeReputationScore() > 0L

    // If we have set devModeReputationScore and if we are in dev mode we override the real reputation score.
    // This code would get provided by reputationService.findReputationScore but as we use the observer and the MutableStateFlow
    // We do not get not existing reputation values, thus we make the call here and provide the result to the MutableStateFlow field.
    private fun provideDevModeReputationScore(userProfileId: String): ReputationScoreVO? {
        return reputationService.findReputationScore(userProfileId)?.get()?.let {
            Mappings.ReputationScoreMapping.fromBisq2Model(it)
        }?.also {
            _reputationByUserProfileId.update { current ->
                current + (userProfileId to it)
            }
        }
    }

    // Private
    private fun observeReputation() {
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