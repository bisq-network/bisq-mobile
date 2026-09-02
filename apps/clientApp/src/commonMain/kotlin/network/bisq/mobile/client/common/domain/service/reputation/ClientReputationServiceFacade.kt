package network.bisq.mobile.client.common.domain.service.reputation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.subscription.collectPayloads
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.utils.resultCatching

class ClientReputationServiceFacade(
    val apiGateway: ReputationApiGateway,
    private val json: Json,
    // Seam for tests: BuildConfig.IS_DEBUG is a compile-time const, so both getReputation
    // branches cannot be exercised without injecting the flag.
    private val isDebug: Boolean = BuildConfig.IS_DEBUG,
) : ServiceFacade(),
    ReputationServiceFacade {
    // MutableStateFlow is only used as there is no kmp compatible concurrent map. The ConcurrentMap from ktor is not recommended to be
    // used as its only an internal implementation.
    // reputationByUserProfileId is used only as local cache to avoid frequent API calls.
    private val reputationByUserProfileId = MutableStateFlow<Map<String, ReputationScoreVO>>(emptyMap())

    // Properties
    // Kept alongside the VO cache rather than derived with stateIn(serviceScope): that scope is
    // cancelled on deactivate, which would leave a derived flow silent for the rest of the process.
    // Both are written together, in subscribeReputation.
    private val _scoreByUserProfileId = MutableStateFlow<Map<String, Long>>(emptyMap())
    override val scoreByUserProfileId: StateFlow<Map<String, Long>> = _scoreByUserProfileId.asStateFlow()

    // Life cycle
    override suspend fun activate() {
        super<ServiceFacade>.activate()
        serviceScope.launch {
            resultCatching {
                subscribeReputation()
            }.onFailure {
                log.w { "Failed to activate client reputation service" }
            }
        }
    }

    override suspend fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun getProfileAge(userProfileId: String): Result<Long?> =
        resultCatching {
            apiGateway.getProfileAge(userProfileId).getOrThrow()
        }.onFailure { e -> log.e(e) { "Failed to get profile age" } }

    // API
    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        // We do not have access to the config data, thus we check with isDebug if we are in dev mode and if so,
        // we request the reputation score from the API instead of looking up the MutableStateFlow field which would contain only
        // scores of profiles which have real reputation. By calling the getReputationScore on the backend we will get the
        // devModeReputationScore in case the user has set that at the backend apps config and is in devMode.
        if (isDebug) {
            return apiGateway.getReputationScore(userProfileId)
        }
        return reputationByUserProfileId.value[userProfileId]?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Reputation not found"))
    }

    // Private
    private suspend fun subscribeReputation() {
        val observer = apiGateway.subscribeUserReputation()
        observer.collectPayloads<Map<String, ReputationScoreVO>>(json) { payload, _ ->
            reputationByUserProfileId.value = payload
            _scoreByUserProfileId.value = payload.mapValues { (_, v) -> v.totalScore }
        }
    }
}
