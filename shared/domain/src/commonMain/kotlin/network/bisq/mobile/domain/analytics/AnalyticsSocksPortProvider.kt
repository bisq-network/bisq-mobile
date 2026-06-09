package network.bisq.mobile.domain.analytics

/**
 * Resolves the local SOCKS5 listener port that the Sentry SDK should route
 * through. Each app supplies the implementation backed by ITS Tor instance:
 *
 *  - **Connect (client app)**: backed by `KmpTorService`, which only starts
 *    Tor when the user pairs an onion trusted node. On a LAN/clearnet trusted
 *    node, kmp-tor is never started — [awaitSocksPort] suspends forever and
 *    Sentry is never initialized (events sit in the bounded buffer and evict
 *    naturally, no clearnet leak).
 *
 *  - **Node app (Easy)**: backed by the bisq2 embedded `NetworkService`,
 *    which always starts Tor for its own p2p networking. The implementation
 *    polls bisq2 until the SOCKS proxy becomes available — typically within
 *    a minute of app launch.
 *
 * The abstraction matters because the two backends look nothing alike: kmp-tor
 * exposes a `StateFlow<Int?>` of the bound port; bisq2 exposes a synchronous
 * `Optional<Socks5Proxy>` once its `NetworkService` is initialized. Hiding
 * that behind a single `suspend fun` keeps `ApplicationLifecycleService` free
 * of platform conditionals.
 *
 * ## Contract
 *  - Implementations MUST suspend until a SOCKS port is genuinely bound and
 *    listening. Returning a stale or speculative port would silently fail the
 *    Sentry init (or worse, send envelopes to a closed socket and burn buffer
 *    slots on guaranteed-failed retries).
 *  - Implementations MAY suspend indefinitely. The caller's CoroutineScope
 *    cancellation is the only exit signal — no internal timeout.
 *  - Cancellation MUST be honoured promptly. The lifecycle uses serviceScope
 *    which is cancelled on app shutdown; a stuck implementation would leak.
 */
interface AnalyticsSocksPortProvider {
    suspend fun awaitSocksPort(): Int
}
