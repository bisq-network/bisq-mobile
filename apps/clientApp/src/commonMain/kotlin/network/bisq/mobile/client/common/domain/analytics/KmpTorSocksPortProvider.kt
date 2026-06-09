package network.bisq.mobile.client.common.domain.analytics

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.domain.analytics.AnalyticsSocksPortProvider

/**
 * [AnalyticsSocksPortProvider] for the Connect (client) app. Observes
 * `KmpTorService.socksPort` and suspends until kmp-tor publishes a non-null
 * port — i.e. until the user pairs an onion trusted node and
 * `TrustedNodeSetupUseCase` calls `startTor()`.
 *
 * Why not `KmpTorService.awaitSocksPort()`? That helper returns null immediately
 * if the Tor state is currently `Stopped` — meaning the caller has to retry
 * itself. Phase 1's analytics gate wants a single "wait forever" semantic, and
 * the cleanest way to get it is to subscribe directly to the `socksPort` flow
 * with no cancel-on-stop signal mixed in.
 *
 * On a LAN/clearnet trusted node, kmp-tor is never started, so this provider
 * suspends indefinitely — by design. Events accumulate in
 * `BufferedAnalyticsService` and evict via the bounded FIFO. No clearnet leak.
 */
class KmpTorSocksPortProvider(
    private val kmpTorService: KmpTorService,
) : AnalyticsSocksPortProvider {
    override suspend fun awaitSocksPort(): Int = kmpTorService.socksPort.filterNotNull().first()
}
