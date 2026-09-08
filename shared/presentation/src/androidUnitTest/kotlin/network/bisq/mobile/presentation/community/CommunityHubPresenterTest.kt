package network.bisq.mobile.presentation.community

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.fixtures.testCommunityHubService
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityHubPresenterTest : PresentationKoinTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var capabilitiesFlow: MutableStateFlow<BackendCapabilities>

    override fun onKoinReady() {
        mainPresenter = mockk(relaxed = true)
        capabilitiesFlow = MutableStateFlow(BackendCapabilities.UNAVAILABLE)
    }

    private fun kotlinx.coroutines.test.TestScope.createAttachedPresenter(
        enabled: Set<CommunitySegment> = emptySet(),
        requiredFeatures: Map<CommunitySegment, Feature> = emptyMap(),
    ): CommunityHubPresenter {
        val hubService =
            testCommunityHubService(
                enabled = enabled,
                requiredFeatures = requiredFeatures,
                capabilities = capabilitiesFlow,
                dispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        val presenter = CommunityHubPresenter(mainPresenter, hubService)
        presenter.onViewAttached()
        advanceUntilIdle()
        return presenter
    }

    @Test
    fun `per-segment unread counts are seeded synchronously and updated live`() =
        runTest {
            val hubService =
                testCommunityHubService(
                    enabled = setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES),
                    requiredFeatures = emptyMap(),
                    capabilities = capabilitiesFlow,
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                )
            hubService.setUnreadCounts(mapOf(CommunitySegment.MESSAGES to 3))

            val presenter = CommunityHubPresenter(mainPresenter, hubService)
            // Seeded before any collector runs, so the tab pills render on the first frame.
            assertEquals(mapOf(CommunitySegment.MESSAGES to 3), presenter.uiState.value.segmentUnreadCounts)

            presenter.onViewAttached()
            advanceUntilIdle()
            hubService.setUnreadCounts(mapOf(CommunitySegment.DISCUSSIONS to 2, CommunitySegment.MESSAGES to 1))
            advanceUntilIdle()

            assertEquals(
                mapOf(CommunitySegment.DISCUSSIONS to 2, CommunitySegment.MESSAGES to 1),
                presenter.uiState.value.segmentUnreadCounts,
            )
            presenter.onViewUnattaching()
        }

    @Test
    fun `live segments render in declaration order with the first selected`() =
        runTest {
            val presenter =
                createAttachedPresenter(enabled = setOf(CommunitySegment.MESSAGES, CommunitySegment.DISCUSSIONS))

            assertEquals(
                listOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES),
                presenter.uiState.value.liveSegments,
            )
            assertEquals(CommunitySegment.DISCUSSIONS, presenter.uiState.value.selectedSegment)
        }

    @Test
    fun `selecting a live segment updates the selection`() =
        runTest {
            val presenter =
                createAttachedPresenter(enabled = setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES))

            presenter.onAction(CommunityHubUiAction.OnSegmentSelect(CommunitySegment.MESSAGES))

            assertEquals(CommunitySegment.MESSAGES, presenter.uiState.value.selectedSegment)
        }

    @Test
    fun `selecting a segment that is not live is ignored`() =
        runTest {
            val presenter = createAttachedPresenter(enabled = setOf(CommunitySegment.DISCUSSIONS))

            presenter.onAction(CommunityHubUiAction.OnSegmentSelect(CommunitySegment.CONTACTS))

            assertEquals(CommunitySegment.DISCUSSIONS, presenter.uiState.value.selectedSegment)
        }

    @Test
    fun `selection falls back to the first live segment when the selected one goes away`() =
        runTest {
            val presenter =
                createAttachedPresenter(
                    enabled = setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES),
                    requiredFeatures = mapOf(CommunitySegment.MESSAGES to Feature.NETWORK_INFO),
                )
            capabilitiesFlow.value = BackendCapabilities(setOf(Feature.NETWORK_INFO.key))
            advanceUntilIdle()
            presenter.onAction(CommunityHubUiAction.OnSegmentSelect(CommunitySegment.MESSAGES))
            assertEquals(CommunitySegment.MESSAGES, presenter.uiState.value.selectedSegment)

            capabilitiesFlow.value = BackendCapabilities.UNAVAILABLE
            advanceUntilIdle()

            assertEquals(listOf(CommunitySegment.DISCUSSIONS), presenter.uiState.value.liveSegments)
            assertEquals(CommunitySegment.DISCUSSIONS, presenter.uiState.value.selectedSegment)
        }

    @Test
    fun `no live segments means no selection`() =
        runTest {
            val presenter = createAttachedPresenter()

            assertEquals(emptyList(), presenter.uiState.value.liveSegments)
            assertNull(presenter.uiState.value.selectedSegment)
        }

    /**
     * Support is pushed as its own screen rather than selected as a segment: it is not one, and the
     * hub keeps the segment it was on so backing out lands where the user left.
     */
    @Test
    fun `the support quick access pushes the support channel and keeps the segment`() =
        runTest {
            val presenter = createAttachedPresenter(enabled = setOf(CommunitySegment.DISCUSSIONS))

            presenter.onAction(CommunityHubUiAction.OnOpenSupportChannel)
            advanceUntilIdle()

            verify { navigationManager.navigate(NavRoute.SupportChannel, any(), any()) }
            assertEquals(CommunitySegment.DISCUSSIONS, presenter.uiState.value.selectedSegment)
        }

    @Test
    fun `deep-link segment is selected when already live`() =
        runTest {
            val presenter =
                createAttachedPresenter(enabled = setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.CONTACTS))

            presenter.selectInitialSegment(CommunitySegment.CONTACTS)

            assertEquals(CommunitySegment.CONTACTS, presenter.uiState.value.selectedSegment)
        }

    @Test
    fun `deep-link segment is honored once it becomes live, then only once`() =
        runTest {
            // CONTACTS is enabled but gated on a backend feature that is not supported yet.
            val presenter =
                createAttachedPresenter(
                    enabled = setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.CONTACTS),
                    requiredFeatures = mapOf(CommunitySegment.CONTACTS to Feature.NETWORK_INFO),
                )
            presenter.selectInitialSegment(CommunitySegment.CONTACTS)
            assertEquals(CommunitySegment.DISCUSSIONS, presenter.uiState.value.selectedSegment)

            capabilitiesFlow.value = BackendCapabilities(supportedFeatures = setOf(Feature.NETWORK_INFO.key))
            advanceUntilIdle()
            assertEquals(CommunitySegment.CONTACTS, presenter.uiState.value.selectedSegment)

            // The deep-link must not re-assert itself over the user's later choice.
            presenter.onAction(CommunityHubUiAction.OnSegmentSelect(CommunitySegment.DISCUSSIONS))
            capabilitiesFlow.value = BackendCapabilities(supportedFeatures = setOf(Feature.NETWORK_INFO.key, "x"))
            advanceUntilIdle()
            assertEquals(CommunitySegment.DISCUSSIONS, presenter.uiState.value.selectedSegment)
        }
}
