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
 * Pins the gating composition rule: `liveSegments = (shipped ∪ devForced) ∩ capabilities`,
 * fail closed on a missing backend capability, plus the dev-override parser and the
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
    fun `no shipped and no forced segments means nothing is live`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    shippedSegments = emptySet(),
                    devForcedSegments = emptySet(),
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(emptySet(), state.liveSegments.value)
        }

    @Test
    fun `shipped segment without a backend requirement is live`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    shippedSegments = setOf(CommunitySegment.DISCUSSIONS),
                    devForcedSegments = emptySet(),
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(setOf(CommunitySegment.DISCUSSIONS), state.liveSegments.value)
        }

    @Test
    fun `shipped segment with an unsupported backend feature is gated off`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    shippedSegments = setOf(CommunitySegment.DISCUSSIONS),
                    devForcedSegments = emptySet(),
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
                    shippedSegments = setOf(CommunitySegment.DISCUSSIONS),
                    devForcedSegments = emptySet(),
                    requiredFeatures = mapOf(CommunitySegment.DISCUSSIONS to Feature.NETWORK_INFO),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(emptySet(), state.liveSegments.value)

            capabilities.flow.value = supporting(Feature.NETWORK_INFO)

            assertEquals(setOf(CommunitySegment.DISCUSSIONS), state.liveSegments.value)
        }

    @Test
    fun `dev forced segments are unioned with shipped ones`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    shippedSegments = setOf(CommunitySegment.DISCUSSIONS),
                    devForcedSegments = setOf(CommunitySegment.MESSAGES),
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES), state.liveSegments.value)
        }

    @Test
    fun `dev forced segment does not bypass the capability gate`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    shippedSegments = emptySet(),
                    devForcedSegments = setOf(CommunitySegment.MESSAGES),
                    requiredFeatures = mapOf(CommunitySegment.MESSAGES to Feature.NETWORK_INFO),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(emptySet(), state.liveSegments.value)
        }

    @Test
    fun `parse accepts empty and blank input as no segments`() {
        assertEquals(emptySet(), CommunityHubService.parseDevForcedSegments("", propertyName = "test.prop"))
        assertEquals(emptySet(), CommunityHubService.parseDevForcedSegments("  ", propertyName = "test.prop"))
    }

    @Test
    fun `parse is case insensitive and trims entries`() {
        assertEquals(
            setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES),
            CommunityHubService.parseDevForcedSegments(" discussions , MESSAGES ", propertyName = "test.prop"),
        )
    }

    @Test
    fun `parse fails fast on an unknown segment name`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                CommunityHubService.parseDevForcedSegments("DISCUSSIONS,TYPO", propertyName = "test.prop")
            }
        assertTrue(error.message.orEmpty().contains("test.prop"))
    }

    @Test
    fun `unread count is settable and never negative`() =
        runTest {
            val state =
                CommunityHubService(
                    backendCapabilitiesService = FakeCapabilities(),
                    shippedSegments = emptySet(),
                    devForcedSegments = emptySet(),
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            assertEquals(0, state.unreadCount.value)
            state.setUnreadCount(7)
            assertEquals(7, state.unreadCount.value)
            state.setUnreadCount(-3)
            assertEquals(0, state.unreadCount.value)
        }
}
