package network.bisq.mobile.client.common.domain.service.config

import io.ktor.http.HttpStatusCode
import io.ktor.http.parseUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.service.settings.SettingsApiGateway
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.config.ConfigServiceFacade

/**
 * Client implementation of [ConfigServiceFacade].
 *
 * Static config changes only with the trusted node's API version, so we cache it on disk keyed by
 * (node host, api version) and follow a stale-while-revalidate flow on [activate]:
 *  1. surface the cached value immediately for fast render;
 *  2. read the node's current API version; if it matches the cache we skip the fetch entirely;
 *  3. otherwise fetch, emit, and re-persist tagged with the new version.
 *
 * The value always resolves to something usable: a genuine 404 (older node without the endpoint)
 * caches [TradeAmountLimitsVO.DEFAULT] for that version so we don't re-hit it; a transient failure
 * keeps the last good value and retries on the next bootstrap.
 */
class ClientConfigServiceFacade(
    private val configApiGateway: ConfigApiGateway,
    private val settingsApiGateway: SettingsApiGateway,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val configCacheRepository: ConfigCacheRepository,
) : ServiceFacade(),
    ConfigServiceFacade {
    private val _tradeAmountLimits = MutableStateFlow(TradeAmountLimitsVO.DEFAULT)
    override val tradeAmountLimits: StateFlow<TradeAmountLimitsVO> = _tradeAmountLimits.asStateFlow()

    override suspend fun activate() {
        super<ServiceFacade>.activate()
        serviceScope.launch { loadConfig() }
    }

    private suspend fun loadConfig() {
        val hostHash = parseUrl(sensitiveSettingsRepository.fetch().bisqApiUrl)?.host?.let { hashTrustedNodeHost(it) }

        val cached = runCatching { configCacheRepository.get() }.getOrNull()
        // A cache from a different node is invalid: drop it from disk and don't serve it, so we never
        // show one node's config against another.
        val validCached = cached?.takeIf { it.trustedNodeHostHash == hostHash }
        if (cached != null && validCached == null) {
            log.d { "Config: cached entry belongs to a different node; invalidating" }
            runCatching { configCacheRepository.clear() }
        }
        // Stale-while-revalidate: show the current node's last good value instantly so the screen never
        // waits on a fetch.
        validCached?.let { _tradeAmountLimits.value = it.tradeAmountLimits }

        val version = settingsApiGateway.getApiVersion().getOrNull()?.version
        if (version == null) {
            // Node unreachable / version unknown — can't validate freshness. Keep the cached value (or
            // DEFAULT) and retry on the next bootstrap.
            log.d { "Config: node version unavailable; keeping ${if (validCached != null) "cached" else "default"} limits" }
            return
        }

        if (validCached != null && validCached.apiVersion == version) {
            log.d { "Config: cache hit for $version; skipping fetch" }
            return
        }

        configApiGateway
            .getTradeAmountLimits()
            .onSuccess { limits ->
                _tradeAmountLimits.value = limits
                persist(hostHash, version, limits)
            }.onFailure { e ->
                if (e.isEndpointAbsent()) {
                    // Definitive: this node/version has no config endpoint. Expose the safe default and
                    // cache it for the version so the 404 isn't re-hit until the node upgrades.
                    _tradeAmountLimits.value = TradeAmountLimitsVO.DEFAULT
                    persist(hostHash, version, TradeAmountLimitsVO.DEFAULT)
                } else {
                    // Transient (timeout / connection). Don't cache; keep the cached/DEFAULT value and
                    // retry on the next bootstrap.
                    log.d(e) { "Config: transient fetch failure; will retry next bootstrap" }
                }
            }
    }

    private suspend fun persist(
        hostHash: String?,
        version: String,
        limits: TradeAmountLimitsVO,
    ) {
        // No stable host means no stable cache key (unpaired / malformed url) — skip caching, just emit.
        if (hostHash == null) return
        configCacheRepository.set(ConfigCacheEntry(hostHash, version, limits))
    }

    private fun Throwable.isEndpointAbsent(): Boolean = this is WebSocketRestApiException && httpStatusCode == HttpStatusCode.NotFound
}
