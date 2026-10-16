package network.bisq.mobile.client.common.domain.service.config

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.ApiInfoDto
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.domain.service.capabilities.Feature
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ClientConfigServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val configApiGateway: ConfigApiGateway = mockk(relaxed = true)
    private val webSocketClientService: WebSocketClientService = mockk(relaxed = true)
    private val sensitiveSettingsRepository: SensitiveSettingsRepository = mockk(relaxed = true)
    private val cacheRepository = FakeConfigCacheRepository()

    private lateinit var facade: ClientConfigServiceFacade

    private val host = "r7m2xpqowg3bvf8t.onion"
    private val hostHash = hashTrustedNodeHost(host)
    private val version = "2.1.12"
    private val fetched = TradeAmountLimitsVO.DEFAULT.copy(tolerance = 0.1, requiredReputationScorePerUsd = 150.0)
    private val advertised = setOf(Feature.CLOSED_TRADES.key, Feature.NETWORK_INFO.key)

    // Handshake of a node without /info unless a test says otherwise: version only, no features.
    private val nodeInfo = MutableStateFlow<ApiInfoDto?>(ApiInfoDto(version))

    private val settingsFlow =
        MutableStateFlow(SensitiveSettings(bisqApiUrl = "http://$host:8090", selectedProxyOption = BisqProxyOption.INTERNAL_TOR))

    override fun onSetup() {
        every { sensitiveSettingsRepository.data } returns settingsFlow
        coEvery { sensitiveSettingsRepository.fetch() } coAnswers { settingsFlow.value }
        every { webSocketClientService.nodeInfo } returns nodeInfo
        coEvery { configApiGateway.getTradeAmountLimits() } returns Result.success(fetched)
        coEvery { configApiGateway.getCapabilities() } returns Result.success(ApiCapabilitiesDto(version, emptyList()))
        facade = ClientConfigServiceFacade(configApiGateway, webSocketClientService, sensitiveSettingsRepository, cacheRepository)
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
    fun `image version keys the cache instead of the api version when reported`() =
        runTest {
            // Cache from before the node reported an image version (or from a pre-imageVersion
            // node build): the containerised node may have been rebuilt in place, so the core
            // api version alone no longer proves freshness.
            cacheRepository.set(ConfigCacheEntry(hostHash, version, fetched))
            nodeInfo.value = ApiInfoDto(version, imageVersion = "$version.2")

            facade.activate()
            advanceUntilIdle()

            assertEquals(ConfigCacheEntry(hostHash, "$version.2", fetched), cacheRepository.get())
            coVerify(exactly = 1) { configApiGateway.getTradeAmountLimits() }
        }

    @Test
    fun `changed image version under an unchanged core version refetches`() =
        runTest {
            cacheRepository.set(ConfigCacheEntry(hostHash, "$version.1", fetched))
            nodeInfo.value = ApiInfoDto(version, imageVersion = "$version.2")

            facade.activate()
            advanceUntilIdle()

            assertEquals(ConfigCacheEntry(hostHash, "$version.2", fetched), cacheRepository.get())
            coVerify(exactly = 1) { configApiGateway.getTradeAmountLimits() }
        }

    @Test
    fun `same image version hits cache and does not fetch`() =
        runTest {
            cacheRepository.set(ConfigCacheEntry(hostHash, "$version.2", fetched))
            nodeInfo.value = ApiInfoDto(version, imageVersion = "$version.2")

            facade.activate()
            advanceUntilIdle()

            assertEquals(fetched, facade.tradeAmountLimits.value)
            coVerify(exactly = 0) { configApiGateway.getTradeAmountLimits() }
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
    fun `pending handshake serves the cached value and fetches nothing`() =
        runTest {
            val good = TradeAmountLimitsVO.DEFAULT.copy(tolerance = 0.3)
            cacheRepository.set(ConfigCacheEntry(hostHash, "2.1.11", good))
            nodeInfo.value = null

            facade.activate()
            advanceUntilIdle()

            assertEquals(good, facade.tradeAmountLimits.value)
            coVerify(exactly = 0) { configApiGateway.getTradeAmountLimits() }
        }

    @Test
    fun `pending handshake with no cache stays on the bundled default`() =
        runTest {
            nodeInfo.value = null

            facade.activate()
            advanceUntilIdle()

            assertEquals(TradeAmountLimitsVO.DEFAULT, facade.tradeAmountLimits.value)
            assertNull(cacheRepository.get())
            coVerify(exactly = 0) { configApiGateway.getTradeAmountLimits() }
        }

    @Test
    fun `a handshake completing after activate finishes the load`() =
        runTest {
            nodeInfo.value = null
            facade.activate()
            advanceUntilIdle()

            nodeInfo.value = ApiInfoDto(version, features = advertised.toList())
            advanceUntilIdle()

            assertEquals(fetched, facade.tradeAmountLimits.value)
            assertEquals(advertised, facade.supportedFeatures.value)
            assertEquals(ConfigCacheEntry(hostHash, version, fetched, advertised), cacheRepository.get())
        }

    @Test
    fun `handshake features are exposed and cached without fetching the manifest`() =
        runTest {
            nodeInfo.value = ApiInfoDto(version, features = advertised.toList())

            facade.activate()
            advanceUntilIdle()

            assertEquals(advertised, facade.supportedFeatures.value)
            assertEquals(advertised, cacheRepository.get()?.supportedFeatures)
            coVerify(exactly = 0) { configApiGateway.getCapabilities() }
        }

    @Test
    fun `an empty handshake feature list means no features and does not fall back to the manifest`() =
        runTest {
            nodeInfo.value = ApiInfoDto(version, features = emptyList())

            facade.activate()
            advanceUntilIdle()

            assertEquals(emptySet(), facade.supportedFeatures.value)
            assertEquals(emptySet(), cacheRepository.get()?.supportedFeatures)
            coVerify(exactly = 0) { configApiGateway.getCapabilities() }
        }

    @Test
    fun `handshake features matching the cache hit the cache and fetch nothing`() =
        runTest {
            cacheRepository.set(ConfigCacheEntry(hostHash, version, fetched, advertised))
            nodeInfo.value = ApiInfoDto(version, features = advertised.toList())

            facade.activate()
            advanceUntilIdle()

            assertEquals(advertised, facade.supportedFeatures.value)
            coVerify(exactly = 0) { configApiGateway.getTradeAmountLimits() }
            coVerify(exactly = 0) { configApiGateway.getCapabilities() }
        }

    @Test
    fun `handshake features that differ from the cache refetch the limits and re-persist`() =
        runTest {
            // Same version key but a different feature set: the node changed in place, so the cached
            // limits cannot be trusted either.
            val stale = TradeAmountLimitsVO.DEFAULT.copy(tolerance = 0.5)
            cacheRepository.set(ConfigCacheEntry(hostHash, version, stale, setOf(Feature.CLOSED_TRADES.key)))
            nodeInfo.value = ApiInfoDto(version, features = advertised.toList())

            facade.activate()
            advanceUntilIdle()

            assertEquals(advertised, facade.supportedFeatures.value)
            assertEquals(fetched, facade.tradeAmountLimits.value)
            coVerify(exactly = 1) { configApiGateway.getTradeAmountLimits() }
            coVerify(exactly = 0) { configApiGateway.getCapabilities() }
            assertEquals(ConfigCacheEntry(hostHash, version, fetched, advertised), cacheRepository.get())
        }

    @Test
    fun `re-pairing refetches limits and persists the handshake features without the manifest`() =
        runTest {
            cacheRepository.set(ConfigCacheEntry(hostHash, version, fetched, advertised))
            nodeInfo.value = ApiInfoDto(version, features = advertised.toList())
            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 0) { configApiGateway.getTradeAmountLimits() }

            settingsFlow.value = settingsFlow.value.copy(clientId = "fresh-pairing")
            advanceUntilIdle()

            coVerify(exactly = 1) { configApiGateway.getTradeAmountLimits() }
            coVerify(exactly = 0) { configApiGateway.getCapabilities() }
            assertEquals(ConfigCacheEntry(hostHash, version, fetched, advertised), cacheRepository.get())
        }

    @Test
    fun `re-pairing refetches the manifest even when host and version match the cache`() =
        runTest {
            // Cached manifest from before the node was updated in place: same host, same api
            // version, but no private-chat yet.
            cacheRepository.set(ConfigCacheEntry(hostHash, version, fetched, setOf(Feature.CLOSED_TRADES.key)))
            val updated = setOf(Feature.CLOSED_TRADES.key, Feature.NETWORK_INFO.key)
            coEvery { configApiGateway.getCapabilities() } returns Result.success(ApiCapabilitiesDto(version, updated.toList()))

            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 0) { configApiGateway.getCapabilities() }

            settingsFlow.value = settingsFlow.value.copy(clientId = "fresh-pairing")
            advanceUntilIdle()

            assertEquals(updated, facade.supportedFeatures.value)
            coVerify(exactly = 1) { configApiGateway.getCapabilities() }
            assertEquals(ConfigCacheEntry(hostHash, version, fetched, updated), cacheRepository.get())
        }

    @Test
    fun `a failing cache write does not stop later reloads`() =
        runTest {
            cacheRepository.failOnSet = true

            facade.activate()
            advanceUntilIdle()
            settingsFlow.value = settingsFlow.value.copy(clientId = "fresh-pairing")
            advanceUntilIdle()

            coVerify(exactly = 2) { configApiGateway.getTradeAmountLimits() }
        }

    @Test
    fun `settings changes without a new client id do not drop the cache`() =
        runTest {
            cacheRepository.set(ConfigCacheEntry(hostHash, version, fetched, setOf(Feature.CLOSED_TRADES.key)))

            facade.activate()
            advanceUntilIdle()
            settingsFlow.value = settingsFlow.value.copy(clientName = "renamed")
            advanceUntilIdle()

            coVerify(exactly = 0) { configApiGateway.getCapabilities() }
            assertEquals(setOf(Feature.CLOSED_TRADES.key), facade.supportedFeatures.value)
        }
}

private class FakeConfigCacheRepository : ConfigCacheRepository {
    private var entry: ConfigCacheEntry? = null
    var failOnSet = false

    override suspend fun get(): ConfigCacheEntry? = entry

    override suspend fun set(entry: ConfigCacheEntry) {
        check(!failOnSet) { "disk write failed" }
        this.entry = entry
    }

    override suspend fun clear() {
        entry = null
    }
}
