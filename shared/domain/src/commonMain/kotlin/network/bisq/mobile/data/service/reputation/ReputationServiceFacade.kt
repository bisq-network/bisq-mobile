package network.bisq.mobile.data.service.reputation

import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.LifeCycleAware

interface ReputationServiceFacade : LifeCycleAware {
    val scoreByUserProfileId: Map<String, Long>

    suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO>

    /**
     * Get the profile age (creation timestamp) for a user profile
     * @param userProfileId The user profile ID
     * @return The profile creation timestamp in milliseconds, or null if not available
     */
    suspend fun getProfileAge(userProfileId: String): Result<Long?>
}
