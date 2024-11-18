package network.bisq.mobile.domain.client.main.user_profile

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import network.bisq.mobile.client.user_profile.UserProfileResponse
import network.bisq.mobile.domain.user.profile.UserProfile
import network.bisq.mobile.domain.user_profile.UserProfileModel
import network.bisq.mobile.domain.user_profile.UserProfileServiceFacade
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ClientUserProfileServiceFacade(
    override val model: UserProfileModel,
    private val apiGateway: UserProfileApiGateway
) :
    UserProfileServiceFacade {
    private val log = Logger.withTag("IosClientUserProfileController")

    // TODO Dispatchers.IO is not supported on iOS. Either customize or find whats on iOS appropriate.
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override suspend fun hasUserProfile(): Boolean {
        return getUserIdentityIds().isNotEmpty()
    }

    override suspend fun generateKeyPair() {
        model as ClientUserProfileModel
        coroutineScope.launch {
            try {
                model.setGenerateKeyPairInProgress(true)
                val ts = Clock.System.now().toEpochMilliseconds()
                val response = apiGateway.requestPreparedData()
                model.preparedDataAsJson = response.first
                val preparedData = response.second

                createSimulatedDelay(Clock.System.now().toEpochMilliseconds() - ts)

                model.keyPair = preparedData.keyPair
                model.proofOfWork = preparedData.proofOfWork
                model.setNym(preparedData.nym)
                model.setId(preparedData.id)
            } catch (e: Exception) {
                log.e { e.toString() }
            } finally {
                model.setGenerateKeyPairInProgress(false)
            }
        }
    }

    override suspend fun createAndPublishNewUserProfile() {
        model as ClientUserProfileModel
        coroutineScope.launch {
            try {
                model.setCreateAndPublishInProgress(true)
                val response: UserProfileResponse =
                    apiGateway.createAndPublishNewUserProfile(
                        model.nickName.value,
                        model.preparedDataAsJson
                    )
                require(model.id.value == response.userProfileId)
                { "userProfileId from model does not match userProfileId from response" }
            } catch (e: Exception) {
                log.e { e.toString() }
            } finally {
                model.setCreateAndPublishInProgress(false)
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

    override suspend fun applySelectedUserProfile() {
        try {
            val userProfile = getSelectedUserProfile()
            model.setNickName(userProfile.nickName)
            model.setNym(userProfile.nym)
            model.setId(userProfile.id)
        } catch (e: Exception) {
            log.e { e.toString() }
        }
    }

    private suspend fun getSelectedUserProfile(): UserProfile {
        return apiGateway.getSelectedUserProfile()
    }

    private suspend fun findUserProfile(id: String): UserProfileModel? {
        TODO("Not yet implemented")
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