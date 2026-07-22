package network.bisq.mobile.client.common.domain.service.config

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.service.settings.SettingsApiGateway
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.replicated.settings.ApiVersionSettingsVO
import network.bisq.mobile.domain.service.capabilities.Feature
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ClientConfigServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val configApiGateway: ConfigApiGateway = mockk(relaxed = true)
    private val settingsApiGateway: SettingsApiGateway = mockk(relaxed = true)
    private val sensitiveSettingsRepository: SensitiveSettingsRepository = mockk(relaxed = true)
    private val cacheRepository = FakeConfigCacheRepository()

    private lateinit var facade: ClientConfigServiceFacade

    private val host = "r7m2xpqowg3bvf8t.onion"
    private val hostHash = hashTrustedNodeHost(host)
    private val version = "2.1.12"
    private val fetched = TradeAmountLimitsVO.DEFAULT.copy(tolerance = 0.1, requiredReputationScorePerUsd = 150.0)

    override fun onSetup() {
        coEvery { sensitiveSettingsRepository.fetch() } returns
            SensitiveSettings(bisqApiUrl = "http://$host:8090", selectedProxyOption = BisqProxyOption.INTERNAL_TOR)
        coEvery { settingsApiGateway.getApiVersion() } returns Result.success(ApiVersionSettingsVO(version))
        coEvery { configApiGateway.getTradeAmountLimits() } returns Result.success(fetched)
        coEvery { configApiGateway.getCapabilities() } returns Result.success(ApiCapabilitiesDto(version, emptyList()))
        facade = ClientConfigServiceFacade(configApiGateway, settingsApiGateway, sensitiveSettingsRepository, cacheRepository)
    }

    @Test
    fun `exposes the bundled default before activate`() {
        assertEquals(TradeAmountLimitsVO.DEFAULT, facade.tradeAmountLimits.value)
    }

    @Test
    fun `cold start with empty cache fetches, emits and persists tagged with host and version`() =
        runTest {
            facade.activate()
            advanceUntilIdle()

            assertEquals(fetched, facade.tradeAmountLimits.value)
            assertEquals(ConfigCacheEntry(hostHash, version, fetched), cacheRepository.get())
            coVerify(exactly = 1) { configApiGateway.getTradeAmountLimits() }
        }

    @Test
    fun `same host and version hits cache and does not fetch`() =
        runTest {
            cacheRepository.set(ConfigCacheEntry(hostHash, version, fetched))

            facade.activate()
            advanceUntilIdle()

            assertEquals(fetched, facade.tradeAmountLimits.value)
            coVerify(exactly = 0) { configApiGateway.getTradeAmountLimits() }
        }

    @Test
    fun `changed api version refetches and repersists`() =
        runTest {
            val stale = TradeAmountLimitsVO.DEFAULT.copy(tolerance = 0.9)
            cacheRepository.set(ConfigCacheEntry(hostHash, "2.1.11", stale))

            facade.activate()
            advanceUntilIdle()

            assertEquals(fetched, facade.tradeAmountLimits.value)
            assertEquals(ConfigCacheEntry(hostHash, version, fetched), cacheRepository.get())
            coVerify(exactly = 1) { configApiGateway.getTradeAmountLimits() }
        }

    @Test
    fun `different host refetches even when version matches`() =
        runTest {
            cacheRepository.set(ConfigCacheEntry(hashTrustedNodeHost("other.onion"), version, fetched))

            facade.activate()
            advanceUntilIdle()

            assertEquals(hostHash, cacheRepository.get()?.trustedNodeHostHash)
            coVerify(exactly = 1) { configApiGateway.getTradeAmountLimits() }
        }

    @Test
    fun `cold start fetches the capabilities manifest and exposes and caches the features`() =
        runTest {
            coEvery { configApiGateway.getCapabilities() } returns
                Result.success(ApiCapabilitiesDto(version, listOf("closed-trades", "network-info")))

            facade.activate()
            advanceUntilIdle()

            assertEquals(setOf("closed-trades", "network-info"), facade.supportedFeatures.value)
            assertEquals(setOf("closed-trades", "network-info"), cacheRepository.get()?.supportedFeatures)
        }

    @Test
    fun `capabilities endpoint 404 falls back to the legacy baseline and caches it`() =
        runTest {
            coEvery { configApiGateway.getCapabilities() } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.NotFound, "no manifest"))

            facade.activate()
            advanceUntilIdle()

            // Node predates the manifest but may still run pre-manifest features (closed-trades): assume
            // the legacy baseline so they aren't hidden, and cache it so we don't re-hit.
            assertEquals(Feature.LEGACY_BASELINE_KEYS, facade.supportedFeatures.value)
            assertEquals(Feature.LEGACY_BASELINE_KEYS, cacheRepository.get()?.supportedFeatures)
        }

    @Test
    fun `non-404 capabilities error keeps the legacy baseline visible`() =
        runTest {
            // Reproduces a node without the endpoint that returns a non-404 error (e.g. a proxy 500):
            // classified transient, so we keep the current value — which is the legacy baseline — and
            // the pre-manifest feature stays visible instead of vanishing.
            coEvery { configApiGateway.getCapabilities() } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.InternalServerError, ""))

            facade.activate()
            advanceUntilIdle()

            assertEquals(Feature.LEGACY_BASELINE_KEYS, facade.supportedFeatures.value)
        }

    @Test
    fun `transient capabilities failure keeps cached features and does not persist`() =
        runTest {
            cacheRepository.set(ConfigCacheEntry(hostHash, "2.1.11", fetched, setOf("closed-trades")))
            coEvery { configApiGateway.getCapabilities() } returns Result.failure(RuntimeException("timeout"))

            facade.activate()
            advanceUntilIdle()

            // Keep the last good feature set; leave the cache untouched so the next bootstrap retries.
            assertEquals(setOf("closed-trades"), facade.supportedFeatures.value)
            assertEquals(ConfigCacheEntry(hostHash, "2.1.11", fetched, setOf("closed-trades")), cacheRepository.get())
        }

    @Test
    fun `host change invalidates the other node's cache and never serves its value`() =
        runTest {
            // Cached config belongs to a different node; even if that node's fetch would fail, we must
            // not show its limits against the currently paired node.
            cacheRepository.set(ConfigCacheEntry(hashTrustedNodeHost("other.onion"), version, fetched))
            coEvery { configApiGateway.getTradeAmountLimits() } returns Result.failure(RuntimeException("timeout"))

            facade.activate()
            advanceUntilIdle()

            assertEquals(TradeAmountLimitsVO.DEFAULT, facade.tradeAmountLimits.value)
            assertNull(cacheRepository.get())
        }

    @Test
    fun `endpoint 404 caches the bundled default for that version`() =
        runTest {
            coEvery { configApiGateway.getTradeAmountLimits() } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.NotFound, "no such endpoint"))

            facade.activate()
            advanceUntilIdle()

            // Older node without the endpoint: expose the safe default, and cache it tagged with the
            // version so we don't re-hit the 404 until the node upgrades.
            assertEquals(TradeAmountLimitsVO.DEFAULT, facade.tradeAmountLimits.value)
            assertEquals(ConfigCacheEntry(hostHash, version, TradeAmountLimitsVO.DEFAULT), cacheRepository.get())
        }

    @Test
    fun `transient fetch failure does not cache and keeps the cached value`() =
        runTest {
            val good = TradeAmountLimitsVO.DEFAULT.copy(tolerance = 0.2)
            cacheRepository.set(ConfigCacheEntry(hostHash, "2.1.11", good)) // older version -> will try to refetch
            coEvery { configApiGateway.getTradeAmountLimits() } returns Result.failure(RuntimeException("timeout"))

            facade.activate()
            advanceUntilIdle()

            // Keep the last good value (better than DEFAULT) and leave the cache untouched so the next
            // bootstrap retries instead of being stuck on an empty result.
            assertEquals(good, facade.tradeAmountLimits.value)
            assertEquals(ConfigCacheEntry(hostHash, "2.1.11", good), cacheRepository.get())
        }

    @Test
    fun `unreachable node keeps cached value without fetching config`() =
        runTest {
            val good = TradeAmountLimitsVO.DEFAULT.copy(tolerance = 0.3)
            cacheRepository.set(ConfigCacheEntry(hostHash, "2.1.11", good))
            coEvery { settingsApiGateway.getApiVersion() } returns Result.failure(RuntimeException("no connection"))

            facade.activate()
            advanceUntilIdle()

            assertEquals(good, facade.tradeAmountLimits.value)
            coVerify(exactly = 0) { configApiGateway.getTradeAmountLimits() }
        }

    @Test
    fun `unreachable node with no cache stays on the bundled default`() =
        runTest {
            coEvery { settingsApiGateway.getApiVersion() } returns Result.failure(RuntimeException("no connection"))

            facade.activate()
            advanceUntilIdle()

            assertEquals(TradeAmountLimitsVO.DEFAULT, facade.tradeAmountLimits.value)
            assertNull(cacheRepository.get())
            coVerify(exactly = 0) { configApiGateway.getTradeAmountLimits() }
        }
}

private class FakeConfigCacheRepository : ConfigCacheRepository {
    private var entry: ConfigCacheEntry? = null

    override suspend fun get(): ConfigCacheEntry? = entry

    override suspend fun set(entry: ConfigCacheEntry) {
        this.entry = entry
    }

    override suspend fun clear() {
        entry = null
    }
}
