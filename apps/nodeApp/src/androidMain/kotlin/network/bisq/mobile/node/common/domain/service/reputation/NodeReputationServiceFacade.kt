package network.bisq.mobile.node.common.domain.service.reputation

import bisq.user.reputation.ReputationScore
import bisq.user.reputation.ReputationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.domain.mapping.Mappings

class NodeReputationServiceFacade(private val applicationService: AndroidApplicationService.Provider) : ServiceFacade(),
    ReputationServiceFacade {
    private val reputationService: ReputationService by lazy { applicationService.reputationService.get() }

    override val scoreByUserProfileId: Map<String, Long> get() = reputationService.scoreByUserProfileId

    // Life cycle
    override suspend fun activate() {
        super<ServiceFacade>.activate()
    }

    override suspend fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    // API
    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        return withContext(Dispatchers.Default) {
            try {
                val score: ReputationScore = reputationService.getReputationScore(userProfileId)
                val scoreVO = Mappings.ReputationScoreMapping.fromBisq2Model(score)
                Result.success(scoreVO)
            } catch (e: Exception) {
                log.e(e) { "Failed to get reputation for userId=$userProfileId" }
                Result.failure(e)
            }
        }
    }

    override suspend fun getProfileAge(userProfileId: String): Result<Long?> {
        return withContext(Dispatchers.Default) {
            try {
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
    }
}