package network.bisq.mobile.client.common.domain.analytics

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.domain.analytics.AnalyticsSocksPortProvider

/**
 * [AnalyticsSocksPortProvider] for the Connect (client) app. Observes
 * `KmpTorService.socksPort` AND `bootstrapProgress` and only returns the port
 * once kmp-tor has BOTH bound its SOCKS listener AND finished bootstrapping
 * (progress 100%).
 *
 * Why both signals? kmp-tor binds the SOCKS listener very early in the
 * bootstrap (around 5%, before any circuits exist). If we hand that port to
 * the Sentry SDK at that point, its first envelope POST goes out over a Tor
 * that isn't ready yet — the request times out at the SOCKS layer 30s later
 * (verified on iOS testing 2026-06-09 — first envelope hit "request timed
 * out" while Tor was at 55%). NSURLSession then caches the failure on the
 * configured proxy and subsequent envelopes inherit the dead state. By
 * suspending until bootstrapProgress reaches 100, the first envelope hits
 * a real Tor circuit and the transport stays healthy.
 *
 * On a LAN/clearnet trusted node, kmp-tor is never started, so this provider
 * suspends indefinitely — by design. Events accumulate in
 * `BufferedAnalyticsService` and evict via the bounded FIFO. No clearnet leak.
 *
 * KNOWN LIMITATION TODO (deferred to a follow-up): if kmp-tor restarts mid-session
 * the SOCKS port changes (`50991` → `51016` in the verifying log). The Sentry
 * SDK is one-shot — its `urlSession` is captured at init time and can't be
 * re-injected — so analytics dies for the rest of the session. Not blocking
 */
class KmpTorSocksPortProvider(
    private val kmpTorService: KmpTorService,
) : AnalyticsSocksPortProvider {
    override suspend fun awaitSocksPort(): Int =
        combine(
            kmpTorService.socksPort.filterNotNull(),
            kmpTorService.bootstrapProgress,
        ) { port, progress -> port to progress }
            .first { (_, progress) -> progress >= FULLY_BOOTSTRAPPED }
            .first

    private companion object {
        /**
         * kmp-tor's bootstrap progress reaches 100 when circuits are usable
         * end-to-end. Below 100 the SOCKS listener is bound but envelope
         * POSTs typically time out.
         */
        const val FULLY_BOOTSTRAPPED = 100
    }
}
