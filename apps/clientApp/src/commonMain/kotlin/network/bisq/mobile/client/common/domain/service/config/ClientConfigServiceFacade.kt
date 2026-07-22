package network.bisq.mobile.client.common.domain.service.config

import io.ktor.http.HttpStatusCode
import io.ktor.http.parseUrl
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import network.bisq.mobile.domain.service.capabilities.Feature

/**
 * Client implementation of [ConfigServiceFacade].
 *
 * Static config (trade-amount limits + the supported-features manifest) changes only with the trusted
 * node's API version, so we cache it on disk keyed by (node host, api version) and follow a
 * stale-while-revalidate flow on [activate]:
 *  1. surface the cached values immediately for fast render;
 *  2. read the node's current API version; if it matches the cache we skip the fetches entirely;
 *  3. otherwise fetch, emit, and re-persist tagged with the new version.
 *
 * Each field resolves independently. A genuine 404 is definitive: trade-amount limits fall back to
 * [TradeAmountLimitsVO.DEFAULT] (still usable), and the features manifest falls back to
 * [Feature.LEGACY_BASELINE_KEYS] so features that predate the manifest stay available (newer/unknown
 * features are absent, hence hidden). A transient failure keeps the last good value and retries on the
 * next bootstrap; we only persist a version's entry once both fields have a definitive answer.
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

    // Start from the legacy baseline so features that predate the manifest (closed-trades) aren't
    // hidden while paired with a node that has them but not yet the /config/capabilities endpoint.
    private val _supportedFeatures = MutableStateFlow(Feature.LEGACY_BASELINE_KEYS)
    override val supportedFeatures: StateFlow<Set<String>> = _supportedFeatures.asStateFlow()

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
        // Stale-while-revalidate: show the current node's last good values instantly.
        validCached?.let {
            _tradeAmountLimits.value = it.tradeAmountLimits
            _supportedFeatures.value = it.supportedFeatures
        }

        val version = settingsApiGateway.getApiVersion().getOrNull()?.version
        if (version == null) {
            // Node unreachable / version unknown — can't validate freshness. Keep the cached values (or
            // defaults) and retry on the next bootstrap.
            log.d { "Config: node version unavailable; keeping ${if (validCached != null) "cached" else "default"} config" }
            return
        }

        if (validCached != null && validCached.apiVersion == version) {
            log.d { "Config: cache hit for $version; skipping fetch" }
            return
        }

        // The two reads are independent — fetch them concurrently so bootstrap pays one Tor round-trip,
        // not two.
        val (limits, features) =
            coroutineScope {
                val limitsDeferred = async { resolveLimits(configApiGateway.getTradeAmountLimits()) }
                val featuresDeferred = async { resolveFeatures(configApiGateway.getCapabilities()) }
                limitsDeferred.await() to featuresDeferred.await()
            }
        _tradeAmountLimits.value = limits.value
        _supportedFeatures.value = features.value

        // Only persist when both fields are definitive — a transient failure on either must not cache a
        // wrong value for the version (it would suppress the retry).
        if (hostHash != null && limits.definitive && features.definitive) {
            configCacheRepository.set(ConfigCacheEntry(hostHash, version, limits.value, features.value))
        }
    }

    private fun resolveLimits(result: Result<TradeAmountLimitsVO>): Resolved<TradeAmountLimitsVO> =
        result.fold(
            onSuccess = { Resolved(it, definitive = true) },
            onFailure = { e ->
                if (e.isEndpointAbsent()) {
                    Resolved(TradeAmountLimitsVO.DEFAULT, definitive = true)
                } else {
                    log.d(e) { "Config: transient trade-amount-limits failure; will retry next bootstrap" }
                    Resolved(_tradeAmountLimits.value, definitive = false)
                }
            },
        )

    private fun resolveFeatures(result: Result<ApiCapabilitiesDto>): Resolved<Set<String>> =
        result.fold(
            onSuccess = { Resolved(it.features.toSet(), definitive = true) },
            onFailure = { e ->
                if (e.isEndpointAbsent()) {
                    // Node predates the manifest: assume the legacy baseline so pre-manifest features
                    // (closed-trades) stay available; newer features are absent, hence hidden.
                    Resolved(Feature.LEGACY_BASELINE_KEYS, definitive = true)
                } else {
                    // Transient (or an ambiguous non-404 error): keep the current value — which is the
                    // legacy baseline until a manifest succeeds — and retry on the next bootstrap.
                    log.d(e) { "Config: transient capabilities failure; will retry next bootstrap" }
                    Resolved(_supportedFeatures.value, definitive = false)
                }
            },
        )

    private fun Throwable.isEndpointAbsent(): Boolean = this is WebSocketRestApiException && httpStatusCode == HttpStatusCode.NotFound

    private data class Resolved<T>(
        val value: T,
        val definitive: Boolean,
    )
}
