package network.bisq.mobile.domain.client.main.user_profile

import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import network.bisq.mobile.client.replicated_model.user.profile.UserProfile
import network.bisq.mobile.client.user_profile.UserProfileResponse
import network.bisq.mobile.domain.user_profile.UserProfileServiceFacade
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ClientUserProfileServiceFacade(private val apiGateway: UserProfileApiGateway) :
    UserProfileServiceFacade {
    private val log = Logger.withTag(this::class.simpleName ?: "UserProfileServiceFacade")

    private var preparedDataAsJson: String? = null

    override suspend fun hasUserProfile(): Boolean {
        return getUserIdentityIds().isNotEmpty()
    }

    override suspend fun generateKeyPair(result: (String, String) -> Unit) {
        try {
            val ts = Clock.System.now().toEpochMilliseconds()
            val response = apiGateway.requestPreparedData()
            preparedDataAsJson = response.first
            val preparedData = response.second

            createSimulatedDelay(Clock.System.now().toEpochMilliseconds() - ts)

            result(preparedData.id, preparedData.nym)
        } catch (e: Exception) {
            log.e { e.toString() }
        }
    }

    override suspend fun createAndPublishNewUserProfile(nickName: String) {
        preparedDataAsJson?.let { preparedDataAsJson ->
            try {
                val response: UserProfileResponse =
                    apiGateway.createAndPublishNewUserProfile(
                        nickName,
                        preparedDataAsJson
                    )
                this.preparedDataAsJson = null
                log.i { "Call to createAndPublishNewUserProfile successful. userProfileId = $response.userProfileId" }
            } catch (e: Exception) {
                log.e { e.toString() }
            }
        }
    }

    override suspend fun getUserIdentityIds(): List<String> {
        return try {
            apiGateway.getUserIdentityIds()
        } catch (e: Exception) {
            log.e { e.toString() }
            emptyList()
        }
    }

    override suspend fun applySelectedUserProfile(result: (String?, String?, String?) -> Unit) {
        try {
            val userProfile = getSelectedUserProfile()
            result(userProfile.nickName, userProfile.nym, userProfile.id)
        } catch (e: Exception) {
            log.e { e.toString() }
        }
    }

    private suspend fun getSelectedUserProfile(): UserProfile {
        return apiGateway.getSelectedUserProfile()
    }

    private suspend fun createSimulatedDelay(requestDuration: Long) {
        // Proof of work creation for difficulty 65536 takes about 50 ms to 100 ms on a 4 GHz Intel Core i7.
        // The API request is likely also quite fast
        // We design a delay of 200 - 1000 ms taking into account a random value and the requestDuration.
        // The delay should avoid a too fast flicker-effect in the UI when recreating the nym,
        // and should make the usage of the proof of work more visible.
        val random: Int = Random.nextInt(800)
        val delayDuration = min(1000.0, max(200.0, (200 + random - requestDuration).toDouble()))
            .toLong()
        delay(delayDuration)
    }
}