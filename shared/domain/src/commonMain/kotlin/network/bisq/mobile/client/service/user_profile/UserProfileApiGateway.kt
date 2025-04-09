package network.bisq.mobile.client.service.user_profile

import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

class UserProfileApiGateway(
    private val webSocketApiClient: WebSocketApiClient
) {
    private val basePath = "user-identities"

    suspend fun ping(): Result<KeyMaterialResponse> {
        return webSocketApiClient.get("$basePath/ping")
    }
    
    suspend fun getKeyMaterial(): Result<KeyMaterialResponse> {
        return webSocketApiClient.get("$basePath/key-material")
    }

    suspend fun createAndPublishNewUserProfile(
        nickName: String,
        keyMaterialResponse: KeyMaterialResponse
    ): Result<CreateUserIdentityResponse> {
        val createUserIdentityRequest = CreateUserIdentityRequest(
            nickName,
            "",
            "",
            keyMaterialResponse
        )
        return webSocketApiClient.post(basePath, createUserIdentityRequest)
    }

    suspend fun getUserIdentityIds(): Result<List<String>> {
        return webSocketApiClient.get("$basePath/ids")
    }

    suspend fun getSelectedUserProfile(): Result<UserProfileVO> {
        return webSocketApiClient.get("$basePath/selected/user-profile")
    }

    suspend fun findUserIdentities(ids: List<String>): Result<List<UserIdentityVO>> {
        // TODO: Unimplemented in bisq2
        // return webSocketApiClient.get("$basePath/list/${ids.joinToString()}")
        return Result.success(emptyList())
    }

}