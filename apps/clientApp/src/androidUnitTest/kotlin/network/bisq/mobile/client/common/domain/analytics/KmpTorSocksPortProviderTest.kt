package network.bisq.mobile.client.common.domain.analytics

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.data.service.network.KmpTorService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the gating behaviour of [KmpTorSocksPortProvider]: it must NOT return
 * the SOCKS port until BOTH the port is bound AND kmp-tor reports bootstrap
 * 100%. This is the load-bearing fix for the iOS-testing race where Sentry
 * initialised at 5% bootstrap and the first envelope timed out.
 *
 * Why not `runTest` + virtual time? The provider uses `combine` which
 * subscribes to its upstream flows on `Dispatchers.Default` internally —
 * outside `TestDispatcher`'s control. `advanceUntilIdle()` therefore doesn't
 * pump those collectors, leading to flaky assertions. Real `runBlocking` +
 * `withTimeoutOrNull` with short physical delays gives deterministic results
 * here at the cost of ~150ms total wall-clock per run.
 */
class KmpTorSocksPortProviderTest {
    private fun providerOver(
        socksPort: MutableStateFlow<Int?>,
        bootstrapProgress: MutableStateFlow<Int>,
    ): KmpTorSocksPortProvider {
        val torService =
            mockk<KmpTorService>().also {
                every { it.socksPort } returns socksPort
                every { it.bootstrapProgress } returns bootstrapProgress
            }
        return KmpTorSocksPortProvider(torService)
    }

    /** Short wall-clock budget — generous enough for Dispatchers.Default scheduling, tight enough to keep the suite fast. */
    private val suspendVerifyTimeoutMs = 150L
    private val resolveTimeoutMs = 2_000L

    @Test
    fun `suspends when only the SOCKS port is bound (bootstrap still in progress)`() =
        runBlocking(Dispatchers.Default) {
            val socksPort = MutableStateFlow<Int?>(9050) // listener bound at ~5% bootstrap
            val bootstrap = MutableStateFlow(5) // mid-bootstrap
            val provider = providerOver(socksPort, bootstrap)

            val result =
                withTimeoutOrNull(suspendVerifyTimeoutMs) {
                    provider.awaitSocksPort()
                }
            assertNull(
                result,
                "port bound but bootstrap<100 must keep the provider suspended — early release would burn envelopes on a half-bootstrapped Tor",
            )
        }

    @Test
    fun `suspends when only bootstrap is complete (no SOCKS port bound)`() =
        runBlocking(Dispatchers.Default) {
            // Defensive case: kmp-tor logically shouldn't report bootstrap=100
            // without a bound listener, but the gate must hold either way.
            val socksPort = MutableStateFlow<Int?>(null)
            val bootstrap = MutableStateFlow(100)
            val provider = providerOver(socksPort, bootstrap)

            val result =
                withTimeoutOrNull(suspendVerifyTimeoutMs) {
                    provider.awaitSocksPort()
                }
            assertNull(result, "bootstrap=100 without a bound port must keep the provider suspended")
        }

    @Test
    fun `returns the bound port once bootstrap reaches 100`() =
        runBlocking(Dispatchers.Default) {
            val socksPort = MutableStateFlow<Int?>(9050)
            val bootstrap = MutableStateFlow(5)
            val provider = providerOver(socksPort, bootstrap)

            val awaiter = async { provider.awaitSocksPort() }
            // Give the combine collector a beat to subscribe before we mutate
            // the state — without this, the StateFlow's initial values race
            // the upstream subscription on Dispatchers.Default.
            delay(50L)
            // Pre-condition: still suspended (bootstrap at 5).
            val stillSuspended =
                withTimeoutOrNull(suspendVerifyTimeoutMs) {
                    if (awaiter.isCompleted) awaiter.await() else null
                }
            assertNull(stillSuspended, "must still be suspended at 5% bootstrap")

            // Tor finishes bootstrapping → predicate satisfied → first() resolves.
            bootstrap.value = 100

            val resolved =
                withTimeoutOrNull(resolveTimeoutMs) { awaiter.await() }
            assertEquals(9050, resolved, "must return the bound port once bootstrap completes")
        }
}
