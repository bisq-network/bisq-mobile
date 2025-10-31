package network.bisq.mobile.android.node.service.reputation

import bisq.user.reputation.ReputationScore
import bisq.user.reputation.ReputationService
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade

class NodeReputationServiceFacade(private val applicationService: AndroidApplicationService.Provider) : ServiceFacade(),
    ReputationServiceFacade {
    private val reputationService: ReputationService by lazy { applicationService.reputationService.get() }

    override val scoreByUserProfileId: Map<String, Long> get() = reputationService.scoreByUserProfileId

    // Life cycle
    override fun activate() {
        super<ServiceFacade>.activate()
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    // API
    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        val score: ReputationScore = reputationService.getReputationScore(userProfileId)
        val scoreVO = Mappings.ReputationScoreMapping.fromBisq2Model(score)
        return Result.success(scoreVO)
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
}