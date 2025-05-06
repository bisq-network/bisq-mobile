package network.bisq.mobile.client.service.reputation

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
    private var offersSequenceNumber = atomic(-1)

    // Life cycle
    override fun activate() {
        super<ServiceFacade>.activate()
        serviceScope.launch {
            subscribeReputation()
        }
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    // API
    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        val reputation = reputationByUserProfileId.value[userProfileId]
        if (reputation == null) {
            return Result.failure(Exception())
        } else {
            return Result.success(reputation)
        }
    }

    // Private
    private suspend fun subscribeReputation() {
        val observer = apiGateway.subscribeUserReputation()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }
            if (offersSequenceNumber.value >= webSocketEvent.sequenceNumber) {
                log.w {
                    "Sequence number is larger or equal than the one we " +
                            "received from the backend. We ignore that event."
                }
                return@collect
            }
            offersSequenceNumber.value = webSocketEvent.sequenceNumber
            val webSocketEventPayload: WebSocketEventPayload<Map<String, ReputationScoreVO>> =
                WebSocketEventPayload.from(json, webSocketEvent)
            val payload: Map<String, ReputationScoreVO> = webSocketEventPayload.payload
            _reputationByUserProfileId.value = payload
        }
    }
}