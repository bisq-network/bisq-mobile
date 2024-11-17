package network.bisq.mobile.domain.client.main.user_profile

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.client.user_profile.UserProfileResponse
import network.bisq.mobile.domain.user_profile.UserProfileFacade
import network.bisq.mobile.domain.user_profile.UserProfileModel

class ClientUserProfileFacade(
    override val model: UserProfileModel,
    private val apiGateway: UserProfileApiGateway
) :
    UserProfileFacade {
    private val log = Logger.withTag("IosClientUserProfileController")

    // TODO Dispatchers.IO is not supported on iOS. Either customize or find whats on iOS appropriate.
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun hasUserProfile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun generateKeyPair() {
        model as ClientUserProfileModel
        coroutineScope.launch {
            try {
                val result = apiGateway.requestPreparedData()
                model.preparedDataAsJson = result.first
                val preparedData = result.second
                model.keyPair = preparedData.keyPair
                model.proofOfWork = preparedData.proofOfWork
                model.setNym(preparedData.nym)
                model.setId(preparedData.id)
            } catch (e: Exception) {
                log.e { e.toString() }
            }
        }
    }

    override suspend fun createAndPublishNewUserProfile() {
        model as ClientUserProfileModel
        coroutineScope.launch {
            try {
                val userProfileResponse: UserProfileResponse =
                    apiGateway.createAndPublishNewUserProfile(
                        model.nickName.value,
                        model.preparedDataAsJson
                    )
                require(model.id.value == userProfileResponse.userProfileId)
                { "userProfileId from model does not match userProfileId from response" }
            } catch (e: Exception) {
                log.e { e.toString() }
            }
        }
    }

    override fun getUserProfiles(): Sequence<UserProfileModel> {
        TODO("Not yet implemented")
    }

    override fun findUserProfile(id: String): UserProfileModel? {
        TODO("Not yet implemented")
    }

}