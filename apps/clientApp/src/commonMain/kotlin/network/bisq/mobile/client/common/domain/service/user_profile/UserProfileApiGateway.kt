package network.bisq.mobile.client.common.domain.service.user_profile

import io.ktor.http.encodeURLPath
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

class UserProfileApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientService: WebSocketClientService,
) {
    private val basePath = "user-identities"
    private val profileBasePath = "user-profiles"

    suspend fun ping(): Result<KeyMaterialResponse> = webSocketApiClient.get("$basePath/ping")

    suspend fun getKeyMaterial(): Result<KeyMaterialResponse> = webSocketApiClient.get("$basePath/key-material")

    suspend fun createAndPublishNewUserProfile(
        nickName: String,
        keyMaterialResponse: KeyMaterialResponse,
    ): Result<CreateUserIdentityResponse> {
        val createUserIdentityRequest =
            CreateUserIdentityRequest(
                nickName,
                "",
                "",
                keyMaterialResponse,
            )
        return webSocketApiClient.post(basePath, createUserIdentityRequest)
    }

    suspend fun updateUserProfile(
        statement: String,
        terms: String,
    ): Result<CreateUserIdentityResponse> {
        val request =
            UpdateUserIdentityRequest(
                terms = terms,
                statement = statement,
            )
        return webSocketApiClient.patch(basePath, request)
    }

    suspend fun getUserIdentityIds(): Result<List<String>> = webSocketApiClient.get("$basePath/ids")

    suspend fun getSelectedUserProfile(): Result<UserProfileVO> = webSocketApiClient.get("$basePath/selected/user-profile")

    suspend fun findUserProfiles(ids: List<String>): Result<List<UserProfileVO>> {
        if (ids.isEmpty()) {
            return Result.success(emptyList())
        }
        return webSocketApiClient.get("$profileBasePath?ids=${ids.joinToString(",")}")
    }

    suspend fun getIgnoredUserIds(): Result<List<String>> = webSocketApiClient.get("$profileBasePath/ignored")

    suspend fun ignoreUser(userId: String): Result<Unit> = webSocketApiClient.post("$profileBasePath/ignore/$userId", "")

    suspend fun undoIgnoreUser(userId: String): Result<Unit> = webSocketApiClient.delete("$profileBasePath/ignore/${userId.encodeURLPath()}")

    suspend fun subscribeNumUserProfiles(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.NUM_USER_PROFILES)

    suspend fun reportUserProfile(
        userId: String,
        message: String,
    ): Result<Unit> {
        val body = ReportUserProfileRequest(message)
        return webSocketApiClient.post("$profileBasePath/report/${userId.encodeURLPath()}", body)
    }

    suspend fun triggerUserActivityDetection(): Result<Unit> = webSocketApiClient.post("$profileBasePath/activity", "")
}
