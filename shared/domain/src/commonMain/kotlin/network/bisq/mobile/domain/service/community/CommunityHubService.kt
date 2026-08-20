package network.bisq.mobile.domain.service.community

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature

/**
 * Single source of truth for which Community hub segments are live, and for the hub's
 * aggregate unread count.
 *
 * `liveSegments = (shipped ∪ devForced) ∩ capabilities`:
 * - **shipped**: segments this app version implements ([SHIPPED_SEGMENTS]).
 * - **devForced**: developer override from `feature.communityHubDevSegments`, defaulting
 *   empty in gradle.properties and set per developer in local.properties, so the gated UI
 *   can be exercised before its features ship. Release builds force it empty at the
 *   BuildConfig level, so it can never reach end users.
 * - **capabilities**: per-segment backend requirement ([REQUIRED_FEATURES]) checked against
 *   the trusted node's capability manifest, fail closed — the same gating the rest of the
 *   app uses via [BackendCapabilitiesService]. A segment with no entry has no backend
 *   dependency. The dev override does not bypass this filter.
 */
class CommunityHubService(
    backendCapabilitiesService: BackendCapabilitiesService,
    private val shippedSegments: Set<CommunitySegment> = SHIPPED_SEGMENTS,
    private val devForcedSegments: Set<CommunitySegment> = devForcedSegmentsFromBuildConfig(),
    private val requiredFeatures: Map<CommunitySegment, Feature> = REQUIRED_FEATURES,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    val liveSegments: StateFlow<Set<CommunitySegment>> =
        backendCapabilitiesService.capabilities
            .map { computeLiveSegments(it) }
            .stateIn(
                scope,
                SharingStarted.Eagerly,
                computeLiveSegments(backendCapabilitiesService.capabilities.value),
            )

    private val _unreadCount = MutableStateFlow(0)

    /**
     * Aggregate unread count across live segments, shown by the hub's entry-point badge.
     * TODO feed from the Discussions unread source once it exists.
     */
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun setUnreadCount(count: Int) {
        _unreadCount.value = count.coerceAtLeast(0)
    }

    private fun computeLiveSegments(capabilities: BackendCapabilities): Set<CommunitySegment> =
        (shippedSegments + devForcedSegments)
            .filterTo(mutableSetOf()) { segment ->
                requiredFeatures[segment]?.let { capabilities.isSupported(it) } ?: true
            }

    companion object {
        /** Segments implemented in this app version. TODO add each segment as its wiring ships. */
        val SHIPPED_SEGMENTS: Set<CommunitySegment> = emptySet()

        /**
         * Backend feature each segment requires from the trusted node; a segment without an
         * entry has no backend dependency. TODO register each segment's feature as it ships.
         */
        val REQUIRED_FEATURES: Map<CommunitySegment, Feature> = emptyMap()

        fun devForcedSegmentsFromBuildConfig(): Set<CommunitySegment> = parseDevForcedSegments(BuildConfig.COMMUNITY_HUB_DEV_SEGMENTS)

        /**
         * Parses a comma-separated list of [CommunitySegment] names, case-insensitively,
         * ignoring surrounding whitespace. Unknown names fail fast — this only ever parses
         * a developer-supplied build property.
         */
        fun parseDevForcedSegments(raw: String): Set<CommunitySegment> =
            raw
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { name ->
                    requireNotNull(CommunitySegment.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }) {
                        "Unknown community segment '$name' in feature.communityHubDevSegments; " +
                            "valid values: ${CommunitySegment.entries.joinToString()}"
                    }
                }.toSet()
    }
}
