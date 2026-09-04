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
    fun `parse accepts empty and blank input as no segments`() {
        assertEquals(emptySet(), CommunityHubService.parseSegments("", propertyName = "test.prop"))
        assertEquals(emptySet(), CommunityHubService.parseSegments("  ", propertyName = "test.prop"))
    }

    @Test
    fun `parse is case insensitive and trims entries`() {
        assertEquals(
            setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES),
            CommunityHubService.parseSegments(" discussions , MESSAGES ", propertyName = "test.prop"),
        )
    }

    @Test
    fun `parse fails fast on an unknown segment name`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                CommunityHubService.parseSegments("DISCUSSIONS,TYPO", propertyName = "test.prop")
            }
        assertTrue(error.message.orEmpty().contains("test.prop"))
    }

    @Test
    fun `unread count is settable and never negative`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    enabledSegments = emptySet(),
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(0, state.unreadCount.value)
            state.setUnreadCount(7)
            assertEquals(7, state.unreadCount.value)
            state.setUnreadCount(-3)
            assertEquals(0, state.unreadCount.value)
        }

    /**
     * The production mapping, which every other test bypasses by injecting its own — yet it is the
     * line that decides whether Bisq Connect ever shows Discussions.
     */
    @Test
    fun `the production required features gate Discussions on public chat`() {
        // The one key, not the whole map: the class TODO registers a feature per segment as it
        // ships, and a whole-map assertion would break on each of those for no behavioural reason.
        assertEquals(
            Feature.PUBLIC_CHAT,
            CommunityHubService.REQUIRED_FEATURES[CommunitySegment.DISCUSSIONS],
        )
    }
}
