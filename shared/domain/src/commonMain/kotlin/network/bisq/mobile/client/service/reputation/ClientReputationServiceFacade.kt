package network.bisq.mobile.client.service.reputation

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade

class ClientReputationServiceFacade(
    val apiGateway: ReputationApiGateway,
    private val json: Json,
) : ServiceFacade(), ReputationServiceFacade {

    // Properties
    private val _reputationByUserProfileId = MutableStateFlow<Map<String, ReputationScoreVO>>(emptyMap())
    override val reputationByUserProfileId: StateFlow<Map<String, ReputationScoreVO>> get() = _reputationByUserProfileId.asStateFlow()

    // Misc
    private var reputationSequenceNumber = atomic(-1)

    // Life cycle
    override fun activate() {
        super<ServiceFacade>.activate()
        serviceScope.launch {
            runCatching {
                subscribeReputation()
            }.onFailure {
                log.w { "Failed to activate client reputation service" }
            }
        }
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun getProfileAge(userProfileId: String): Result<Long?> {
        return try {
            apiGateway.getProfileAge(userProfileId)
        } catch (e: Exception) {
            log.e(e) { "Failed to get profile age for userId=$userProfileId" }
            Result.failure(e)
        }
    }

    // API
    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        // We do not have access to the config data, thus we check with BuildConfig.IS_DEBUG if we are in dev mode and if so,
        // we request the reputation score from the API instead of looking up the MutableStateFlow field which would contain only
        // scores of profiles which have real reputation. By calling the getReputationScore on the backend we will get the
        // devModeReputationScore in case the user has set that at the backend apps config and is in devMode.
        if (BuildConfig.IS_DEBUG) {
            return apiGateway.getReputationScore(userProfileId)
        }
        return reputationByUserProfileId.value[userProfileId]?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Reputation for userId=$userProfileId not found"))
    }

    // Private
    private suspend fun subscribeReputation() {
        val observer = apiGateway.subscribeUserReputation()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }
            if (reputationSequenceNumber.value >= webSocketEvent.sequenceNumber) {
                log.w {
                    "Sequence number is larger or equal than the one we " + "received from the backend. We ignore that event."
                }
                return@collect
            }
            reputationSequenceNumber.value = webSocketEvent.sequenceNumber

            runCatching {
                WebSocketEventPayload.from<Map<String, ReputationScoreVO>>(json, webSocketEvent).payload
            }.onSuccess { payload ->
                _reputationByUserProfileId.value = payload
            }.onFailure { t ->
                log.e(t) { "Failed to deserialize reputation payload; event ignored." }
            }
        }
    }
}