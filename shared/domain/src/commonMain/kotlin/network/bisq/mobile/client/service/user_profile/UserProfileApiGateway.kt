package network.bisq.mobile.client.service.user_profile

import network.bisq.mobile.client.replicated_model.user.profile.UserProfile
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient

class UserProfileApiGateway(
    private val webSocketApiClient: WebSocketApiClient
) {
    private val basePath = "user-identities"
    suspend fun requestPreparedData(): network.bisq.mobile.domain.replicated.user.identity.PreparedData {
        return webSocketApiClient.get("$basePath/prepared-data")
    }

    suspend fun createAndPublishNewUserProfile(
        nickName: String,
        preparedData: network.bisq.mobile.domain.replicated.user.identity.PreparedData
    ): UserProfileResponse {
        val createUserIdentityRequest = CreateUserIdentityRequest(
            nickName,
            "",
            "",
            preparedData
        )
        return webSocketApiClient.post(basePath, createUserIdentityRequest)
    }

    suspend fun getUserIdentityIds(): List<String> {
        return webSocketApiClient.get("$basePath/ids")
    }

    suspend fun getSelectedUserProfile(): UserProfile {
        return webSocketApiClient.get("$basePath/selected/user-profile")
    }
}