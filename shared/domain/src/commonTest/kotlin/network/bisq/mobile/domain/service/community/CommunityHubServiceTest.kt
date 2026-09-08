package network.bisq.mobile.domain.service.community

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pins the gating composition rule: `liveSegments = enabled ∩ capabilities`,
 * fail closed on a missing backend capability, plus the rollout-config parser and the
 * unread-count slot.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunityHubServiceTest {
    private class FakeCapabilities(
        initial: BackendCapabilities = BackendCapabilities.UNAVAILABLE,
    ) : BackendCapabilitiesService {
        val flow = MutableStateFlow(initial)
        override val capabilities: StateFlow<BackendCapabilities> = flow
    }

    private fun supporting(feature: Feature) = BackendCapabilities(setOf(feature.key))

    @Test
    fun `no enabled segments means nothing is live`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    enabledSegments = emptySet(),
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(emptySet(), state.liveSegments.value)
        }

    @Test
    fun `enabled segment without a backend requirement is live`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    enabledSegments = setOf(CommunitySegment.DISCUSSIONS),
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(setOf(CommunitySegment.DISCUSSIONS), state.liveSegments.value)
        }

    @Test
    fun `enabled segment with an unsupported backend feature is gated off`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    enabledSegments = setOf(CommunitySegment.DISCUSSIONS),
                    requiredFeatures = mapOf(CommunitySegment.DISCUSSIONS to Feature.NETWORK_INFO),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(emptySet(), state.liveSegments.value)
        }

    @Test
    fun `segment goes live when the backend capability arrives`() =
        runTest {
            val capabilities = FakeCapabilities()
            val state =
                CommunityHubService(
                    backendCapabilitiesService = capabilities,
                    enabledSegments = setOf(CommunitySegment.DISCUSSIONS),
                    requiredFeatures = mapOf(CommunitySegment.DISCUSSIONS to Feature.NETWORK_INFO),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(emptySet(), state.liveSegments.value)

            capabilities.flow.value = supporting(Feature.NETWORK_INFO)

            assertEquals(setOf(CommunitySegment.DISCUSSIONS), state.liveSegments.value)
        }

    @Test
    fun `multiple enabled segments are all live`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    enabledSegments = setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES),
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES), state.liveSegments.value)
        }

    @Test
    fun `capability gate only drops the segment that requires the missing feature`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    enabledSegments = setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES),
                    requiredFeatures = mapOf(CommunitySegment.MESSAGES to Feature.NETWORK_INFO),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(setOf(CommunitySegment.DISCUSSIONS), state.liveSegments.value)
        }

    @Test
    fun `per-segment counts drive the aggregate, clamped and never negative`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    enabledSegments = emptySet(),
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(0, state.unreadCount.value)
            assertEquals(emptyMap(), state.segmentUnreadCounts.value)

            state.setUnreadCounts(mapOf(CommunitySegment.DISCUSSIONS to 7, CommunitySegment.MESSAGES to 5))
            assertEquals(12, state.unreadCount.value)
            assertEquals(7, state.segmentUnreadCounts.value[CommunitySegment.DISCUSSIONS])

            // A negative per-segment count clamps to zero rather than eating into the sum.
            state.setUnreadCounts(mapOf(CommunitySegment.DISCUSSIONS to -3, CommunitySegment.MESSAGES to 5))
            assertEquals(5, state.unreadCount.value)
            assertEquals(0, state.segmentUnreadCounts.value[CommunitySegment.DISCUSSIONS])

            state.setUnreadCounts(emptyMap())
            assertEquals(0, state.unreadCount.value)
        }

    /** Two segments each near Int.MAX must clamp the aggregate instead of wrapping negative. */
    @Test
    fun `the aggregate clamps instead of overflowing`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    enabledSegments = emptySet(),
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            state.setUnreadCounts(mapOf(CommunitySegment.DISCUSSIONS to Int.MAX_VALUE, CommunitySegment.MESSAGES to Int.MAX_VALUE))
            assertEquals(Int.MAX_VALUE, state.unreadCount.value)
        }

    /**
     * The production mapping, which every other test bypasses by injecting its own — yet it is the
     * map that decides what Bisq Connect shows against an older trusted node. With the rollout
     * property removed, this capability filter is the ONLY gate, so the whole map is pinned: a
     * segment silently losing its requirement would offer old-node users a tab their node cannot
     * serve.
     */
    @Test
    fun `the production required features gate every segment on its backend capability`() {
        assertEquals(
            mapOf(
                CommunitySegment.DISCUSSIONS to Feature.PUBLIC_CHAT,
                CommunitySegment.MESSAGES to Feature.PRIVATE_CHAT,
                CommunitySegment.CONTACTS to Feature.CONTACTS,
            ),
            CommunityHubService.REQUIRED_FEATURES,
        )
    }
}
