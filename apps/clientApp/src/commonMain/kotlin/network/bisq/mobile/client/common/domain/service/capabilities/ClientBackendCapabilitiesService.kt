package network.bisq.mobile.client.common.domain.service.capabilities

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import network.bisq.mobile.client.common.domain.service.trades.TradesApiGateway
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.utils.Logging

/**
 * Client-app implementation of [BackendCapabilitiesService] that determines
 * support by **probing the actual feature endpoints**, not by reading the
 * trusted node's reported version.
 *
 * Why probing rather than version comparison: the version string is not
 * authoritative — headless / dev / patched builds can ship feature endpoints
 * while still reporting an older version (e.g. a 2.1.10-base branch with the
 * closed-trades API backported). Probing the endpoint reflects the real
 * surface area available to mobile.
 *
 * Each probe issues a minimal-cost request and treats `Result.isSuccess`
 * as supported. Any failure (404 / 405 / parse failure / network error)
 * resolves to unsupported — fail closed.
 *
 * Probes run on every transition into a connected state so capability flags
 * stay current after pairing or reconnect.
 */
class ClientBackendCapabilitiesService(
    private val tradesApiGateway: TradesApiGateway,
    connectivityService: ConnectivityService,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BackendCapabilitiesService,
    Logging {
    private val _capabilities = MutableStateFlow(BackendCapabilities())
    override val capabilities: StateFlow<BackendCapabilities> = _capabilities.asStateFlow()

    private val serviceScope = CoroutineScope(SupervisorJob() + dispatcher)

    init {
        connectivityService.status
            .filter { it.isConnected() }
            .onEach {
                log.d { "Connectivity reached connected state — probing backend capabilities" }
                refresh()
            }.launchIn(serviceScope)
    }

    override suspend fun refresh() {
        val newCapabilities =
            BackendCapabilities(
                hasClosedTradesApi = probeClosedTradesEndpoint(),
            )
        if (newCapabilities != _capabilities.value) {
            log.i { "BackendCapabilities changed: ${_capabilities.value} -> $newCapabilities" }
            _capabilities.value = newCapabilities
        }
    }

    /**
     * Minimal probe for `GET /trades/closed`. Page 1, page size 1 keeps
     * the round-trip cheap on nodes that DO support it. Any failure
     * (404 / 405 / parse / network) resolves to false.
     */
    private suspend fun probeClosedTradesEndpoint(): Boolean =
        tradesApiGateway
            .getClosedTradesPaginated(page = 1, pageSize = 1)
            .also { it.exceptionOrNull()?.let { e -> log.d { "closed-trades probe failed: ${e.message}" } } }
            .isSuccess
}
