package network.bisq.mobile.client.common.domain.service.config

import io.ktor.http.HttpStatusCode
import io.ktor.http.parseUrl
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.config.ConfigServiceFacade
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.utils.resultCatching

/**
 * Client implementation of [ConfigServiceFacade].
 *
 * Static config (trade-amount limits + the supported features) changes only with the trusted node's
 * version, so we cache it on disk keyed by (node host, node version) and follow a
 * stale-while-revalidate flow on [activate]:
 *  1. surface the cached values immediately for fast render;
 *  2. wait for the connect handshake ([WebSocketClientService.nodeInfo]), which carries the node's
 *     version and, on a node with `/info`, its features; those are published right away;
 *  3. if the version (and the handshake features, when present) match the cache we skip the fetches;
 *  4. otherwise fetch, emit, and re-persist tagged with the new version. The `/config/capabilities`
 *     manifest is only requested when the handshake carried no features (node without `/info`).
 *
 * Containerised nodes report a per-image `imageVersion` that advances with every image release even
 * when the core api version does not, so it is the freshness key when present; other nodes only have
 * the api version.
 *
 * A completed (re-)pairing bypasses the version check: the node behind the same host and api version
 * may have been updated in place with a different feature manifest, so pairing drops the cache and
 * revalidates. Pairing is detected as a change of the server-issued client id, which every pairing
 * renews — including a re-pair with the same node.
 *
 * Each field resolves independently. A genuine 404 is definitive: trade-amount limits fall back to
 * [TradeAmountLimitsVO.DEFAULT] (still usable), and the features manifest falls back to
 * [Feature.LEGACY_BASELINE_KEYS] so features that predate the manifest stay available (newer/unknown
 * features are absent, hence hidden). A transient failure keeps the last good value and retries on the
 * next bootstrap; we only persist a version's entry once both fields have a definitive answer.
 */
class ClientConfigServiceFacade(
    private val configApiGateway: ConfigApiGateway,
    private val webSocketClientService: WebSocketClientService,
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
        serviceScope.launch {
            // First emission is startup: load with the cache honored. Every later change of the
            // client id is a completed (re-)pairing: drop the cache so the manifest is refetched
            // even when host and api version are unchanged. collectLatest also cancels a load
            // stranded by the pairing flow's connection churn once the new pairing lands.
            var startedUp = false
            sensitiveSettingsRepository.data
                .map { it.clientId }
                .distinctUntilChanged()
                .collectLatest {
                    if (startedUp) {
                        resultCatching { configCacheRepository.clear() }
                    }
                    startedUp = true
                    loadConfig()
                }
        }
    }

    private suspend fun loadConfig() {
        val hostHash = parseUrl(sensitiveSettingsRepository.fetch().bisqApiUrl)?.host?.let { hashTrustedNodeHost(it) }

        val cached = resultCatching { configCacheRepository.get() }.getOrNull()
        // A cache from a different node is invalid: drop it from disk and don't serve it, so we never
        // show one node's config against another.
        val validCached = cached?.takeIf { it.trustedNodeHostHash == hostHash }
        if (cached != null && validCached == null) {
            log.d { "Config: cached entry belongs to a different node; invalidating" }
            resultCatching { configCacheRepository.clear() }
        }
        // Stale-while-revalidate: show the current node's last good values instantly.
        validCached?.let {
            _tradeAmountLimits.value = it.tradeAmountLimits
            _supportedFeatures.value = it.supportedFeatures
        }

        // Suspends until the current client has connected; the cached values (or defaults) stay in place
        // meanwhile. A re-pair cancels the wait through collectLatest.
        log.d { "Config: waiting for the connect handshake" }
        val info = webSocketClientService.nodeInfo.filterNotNull().first()
        val handshakeFeatures = info.features?.toSet()
        handshakeFeatures?.let { _supportedFeatures.value = it }

        // Handshake features that differ from the cache mean the node changed under the same version
        // key, so the cached limits are not trusted either.
        val version = info.imageVersion ?: info.version
        val featuresMatchCache = handshakeFeatures == null || handshakeFeatures == validCached?.supportedFeatures
        if (validCached != null && validCached.apiVersion == version && featuresMatchCache) {
            log.d { "Config: cache hit for $version; skipping fetch" }
            return
        }

        // The limits fetch is independent of the manifest (when one is needed), so they run concurrently
        // and bootstrap pays one Tor round-trip, not two.
        val (limits, features) =
            coroutineScope {
                val limitsDeferred = async { resolveLimits(configApiGateway.getTradeAmountLimits()) }
                val features =
                    handshakeFeatures?.let { Resolved(it, definitive = true) }
                        ?: resolveFeatures(configApiGateway.getCapabilities())
                limitsDeferred.await() to features
            }
        _tradeAmountLimits.value = limits.value
        _supportedFeatures.value = features.value

        // Only persist when both fields are definitive — a transient failure on either must not cache a
        // wrong value for the version (it would suppress the retry).
        if (hostHash != null && limits.definitive && features.definitive) {
            resultCatching { configCacheRepository.set(ConfigCacheEntry(hostHash, version, limits.value, features.value)) }
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
