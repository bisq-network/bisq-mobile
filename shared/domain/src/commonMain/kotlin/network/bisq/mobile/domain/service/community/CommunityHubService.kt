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
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature

/**
 * Single source of truth for which Community hub segments are live, and for the hub's
 * aggregate unread count.
 *
 * Every segment ships in every build — the per-app rollout property that staged them was removed
 * once all three were implemented — so liveness is the capability filter alone: the per-segment
 * backend requirement ([REQUIRED_FEATURES]) checked against the trusted node's capability
 * manifest, fail closed — the same gating the rest of the app uses via
 * [BackendCapabilitiesService]. A segment with no entry has no backend dependency. On the NODE
 * app this filter passes by construction: requirements are typed [Feature] entries and the
 * node's config facade reports the full Feature key set (it runs the core in-process). There is
 * deliberately no per-device grant filter: every API permission the segments ride on is STANDARD
 * (covered by any pairing), so node capability alone decides — same as closed trades.
 */
class CommunityHubService(
    backendCapabilitiesService: BackendCapabilitiesService,
    // Test seam only: production constructs this with the default (all segments) and narrows
    // nothing; tests narrow it to isolate one segment's behaviour.
    private val enabledSegments: Set<CommunitySegment> = CommunitySegment.entries.toSet(),
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
     * The GLOBAL community unread count shown by the hub's entry-point badge: the sum of the
     * live segments' own unread counts — Discussions once its wiring ships, plus private-DM
     * unread once Messages ships. The math never changes shape as segments go live; it only
     * gains addends. The Support channel is deliberately and permanently excluded: Support is
     * not a segment, and the aggregate stays a strict Discussions+Messages sum. A single
     * aggregate number is ambiguous about WHICH source needs attention — accepted by design;
     * the hub's per-segment tab counts and per-conversation rows resolve it one tap in
     * (the convention mainstream messengers use for their outermost badge).
     *
     * Fed by [CommunityUnreadCountAggregator], which is the single writer.
     */
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun setUnreadCount(count: Int) {
        _unreadCount.value = count.coerceAtLeast(0)
    }

    private fun computeLiveSegments(capabilities: BackendCapabilities): Set<CommunitySegment> =
        enabledSegments
            .filterTo(mutableSetOf()) { segment ->
                requiredFeatures[segment]?.let { capabilities.isSupported(it) } ?: true
            }

    companion object {
        /**
         * Backend feature each segment requires from the trusted node; a segment without an
         * entry has no backend dependency. TODO register each segment's feature as it ships.
         *
         * Only Bisq Connect is filtered by this: on the node every segment holds by construction,
         * because the app embeds the very Bisq 2 that would advertise the capability.
         */
        val REQUIRED_FEATURES: Map<CommunitySegment, Feature> =
            mapOf(
                CommunitySegment.DISCUSSIONS to Feature.PUBLIC_CHAT,
                CommunitySegment.MESSAGES to Feature.PRIVATE_CHAT,
                CommunitySegment.CONTACTS to Feature.CONTACTS,
            )
    }
}
