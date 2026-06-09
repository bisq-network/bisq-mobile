package network.bisq.mobile.node.common.domain.analytics

import bisq.common.network.TransportType
import kotlinx.coroutines.delay
import network.bisq.mobile.domain.analytics.AnalyticsSocksPortProvider
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService

/**
 * [AnalyticsSocksPortProvider] for the bisq-easy Node app. The embedded bisq2
 * stack runs its own Tor instance via `bisq.network.tor.TorService` — kmp-tor
 * is bound in the node app's DI but is NEVER started there (kmp-tor's
 * `startTor()` is only called from the Connect-side `TrustedNodeSetupUseCase`).
 * Polling kmp-tor's flow would suspend forever on the node app — which is
 * exactly what the user hit when they first tested Phase 1 analytics.
 *
 * bisq2's `NetworkService` does not expose an observable SOCKS-port flow; it
 * makes the SOCKS proxy available synchronously via
 * `serviceNodesByTransport.getSocksProxy(TransportType.TOR)` once Tor is
 * bootstrapped. We poll that surface every [POLL_INTERVAL_MS] until it
 * resolves, then return the port.
 *
 * Polling vs reflection-into-bisq2-observables:
 *  - Polling is robust to bisq2 API churn; the `Optional<Socks5Proxy>` contract
 *    has been stable across multiple bisq2 versions and is part of the public
 *    `NetworkService` surface mobile already depends on.
 *  - The cost is one map lookup + an exception throw on the empty-Optional path
 *    every 2s — negligible.
 *  - Bisq2 cold-start to Tor-ready is typically 30–60s on first run; this loop
 *    will tick ~15–30 times before resolving. After that the coroutine
 *    completes and the polling stops.
 *
 * No upper bound on the wait — if bisq2's Tor never bootstraps the user has
 * bigger problems than missing analytics. Cancellation via the lifecycle's
 * serviceScope is the only exit.
 */
class Bisq2SocksPortProvider(
    private val androidApplicationService: AndroidApplicationService,
) : AnalyticsSocksPortProvider,
    Logging {
    override suspend fun awaitSocksPort(): Int {
        while (true) {
            val port = tryReadPort()
            if (port != null) {
                log.i { "Analytics: bisq2 SOCKS port resolved → $port" }
                return port
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    /**
     * Reads the SOCKS port from bisq2 if it's already available. Returns null
     * (caller polls again) when any link in the chain is not yet initialized:
     *  - serviceNodesByTransport map missing the TOR entry (config without TOR)
     *  - getSocksProxy returns empty Optional (Tor not bootstrapped yet)
     *
     * Throwables from inside bisq2 are also caught and treated as "not ready
     * yet" — we don't want a transient bisq2 hiccup to kill the polling loop.
     * Note that bisq2's `getSocksProxy` already swallows IOException internally,
     * so the catch here is for defensive NPE-style races during early init.
     */
    private fun tryReadPort(): Int? =
        try {
            val socks5Proxy =
                androidApplicationService
                    .networkService
                    .serviceNodesByTransport
                    .getSocksProxy(TransportType.TOR)
                    .orElse(null) ?: return null
            socks5Proxy.port
        } catch (e: Exception) {
            log.d { "Analytics: bisq2 SOCKS port not yet ready (${e.message}) — will retry" }
            null
        }

    private companion object {
        /**
         * 2s strikes a balance between snappy detection (a one-second tail
         * latency after Tor bootstraps) and minimal CPU/syscall churn during
         * the typical 30–60s pre-ready window.
         */
        const val POLL_INTERVAL_MS = 2_000L
    }
}
