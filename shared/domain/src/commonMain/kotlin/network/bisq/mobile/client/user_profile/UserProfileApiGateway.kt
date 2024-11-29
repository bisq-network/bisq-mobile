package network.bisq.mobile.client.user_profile

import network.bisq.mobile.client.replicated_model.user.identity.PreparedData
import network.bisq.mobile.client.replicated_model.user.profile.UserProfile
import network.bisq.mobile.client.websocket.rest_api_proxy.WebSocketRestApiClient

class UserProfileApiGateway(
    private val webSocketRestApiClient: WebSocketRestApiClient
) {
    private val basePath = "user-identities"
    suspend fun requestPreparedData(): PreparedData {
        return webSocketRestApiClient.get("$basePath/prepared-data")
    }

    suspend fun createAndPublishNewUserProfile(
        nickName: String,
        preparedData: PreparedData
    ): UserProfileResponse {
        val createUserIdentityRequest = CreateUserIdentityRequest(
            nickName,
            "",
            "",
            preparedData
        )
        return webSocketRestApiClient.post(basePath, createUserIdentityRequest)
    }

    suspend fun getUserIdentityIds(): List<String> {
        return webSocketRestApiClient.get("$basePath/ids")
    }

    suspend fun getSelectedUserProfile(): UserProfile {
        return webSocketRestApiClient.get("$basePath/selected/user-profile")
    }
}