package network.bisq.mobile.domain.client.main.user_profile

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import network.bisq.mobile.client.replicated_model.user.identity.PreparedData
import network.bisq.mobile.client.replicated_model.user.profile.UserProfile
import network.bisq.mobile.client.service.ApiRequestService
import network.bisq.mobile.client.user_profile.UserProfileResponse
import network.bisq.mobile.utils.ByteArrayAsBase64Serializer

class UserProfileApiGateway(
    private val apiRequestService: ApiRequestService
) {
    private val log = Logger.withTag(this::class.simpleName ?: "UserProfileApiGateway")

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonWithByteArraySerializer = Json {
        serializersModule = SerializersModule {
            contextual(ByteArrayAsBase64Serializer)
        }
        ignoreUnknownKeys = true
    }

    suspend fun requestPreparedData(): Pair<String, PreparedData> {
        val response = apiRequestService.get("user-identity/prepared-data")
        return Pair(response, jsonWithByteArraySerializer.decodeFromString<PreparedData>(response))
    }

    suspend fun createAndPublishNewUserProfile(
        nickName: String,
        preparedDataAsJson: String
    ): UserProfileResponse {
        val createUserIdentityRequest = CreateUserIdentityRequest(
            nickName,
            "",
            "",
            preparedDataAsJson
        )
        val response =
            apiRequestService.post("user-identity/user-identities", createUserIdentityRequest)
        return json.decodeFromString(response)
    }

    suspend fun getUserIdentityIds(): List<String> {
        val response = apiRequestService.get("user-identity/ids")
        return json.decodeFromString(response)
    }


    suspend fun getSelectedUserProfile(): UserProfile {
        val response = apiRequestService.get("user-identity/selected/user-profile")
        return jsonWithByteArraySerializer.decodeFromString<UserProfile>(response)
    }
}