package network.bisq.mobile.client.service.reputation

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    override val reputationByUserProfileId: StateFlow<Map<String, ReputationScoreVO>> get() = _reputationByUserProfileId

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

    // API
    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        val reputation = reputationByUserProfileId.value[userProfileId]
        return when {
            BuildConfig.IS_DEBUG -> reputationDevStub(userProfileId)
            reputation == null -> Result.failure(NoSuchElementException("Reputation for userId=$userProfileId not cached yet"))
            else -> Result.success(reputation)
        }
    }

    private fun reputationDevStub(userProfileId: String): Result<ReputationScoreVO> {
        val reputation = reputationByUserProfileId.value[userProfileId]
        // Hardcoded rep for dev/testing
        // val myId = "f346be"
        val myId = "7730e" // replace with mobile User's ID
        val bobId = "e35fe38" // replace with bisq2 user's ID
        return when {
            userProfileId.startsWith(myId) -> {
                Result.success(
                    ReputationScoreVO(
                        totalScore = 7000,  // Default value will be 0, as bisq-mobile user wont have any rep to start with
                        // Try with different values: 0, <1200, 1200, 1200+
                        fiveSystemScore = 3.5, ranking = 10
                    )
                )
            }

            userProfileId.startsWith(bobId) -> {
                Result.success(
                    ReputationScoreVO(
                        totalScore = 10000, // Default value is 0, as devModeReputationScore set is bisq2, is not propagating to mobile.
                        fiveSystemScore = 4.2, ranking = 3
                    )
                )
            }

            reputation == null -> {
                Result.failure(NoSuchElementException("Reputation for userId=$userProfileId not cached yet"))
            }

            else -> {
                log.w { "Dev stuff for $userProfileId not setup, returning current network reputation" }
                Result.success(reputation)
            }
        }
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