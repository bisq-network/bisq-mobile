package network.bisq.mobile.domain.client.main.user_profile

import co.touchlab.kermit.Logger
import network.bisq.mobile.client.replicated_model.user.identity.PreparedData
import network.bisq.mobile.client.replicated_model.user.profile.UserProfile
import network.bisq.mobile.client.service.ApiRequestService
import network.bisq.mobile.client.user_profile.UserProfileResponse

class UserProfileApiGateway(
    private val apiRequestService: ApiRequestService
) {
    private val log = Logger.withTag(this::class.simpleName ?: "UserProfileApiGateway")
    private val basePath = "user-identities"
    suspend fun requestPreparedData(): PreparedData {

        return apiRequestService.get("$basePath/prepared-data")
    }

    suspend fun createAndPublishNewUserProfile(
        nickName: String,
        preparedData: PreparedData
    ):  UserProfileResponse {
        val createUserIdentityRequest = CreateUserIdentityRequest(
            nickName,
            "",
            "",
            preparedData
        )
        return apiRequestService.post(basePath, createUserIdentityRequest)
    }

    suspend fun getUserIdentityIds(): List<String> {
        return apiRequestService.get("$basePath/ids")
    }

    suspend fun getSelectedUserProfile(): UserProfile {
        return apiRequestService.get("$basePath/selected/user-profile")
    }
}