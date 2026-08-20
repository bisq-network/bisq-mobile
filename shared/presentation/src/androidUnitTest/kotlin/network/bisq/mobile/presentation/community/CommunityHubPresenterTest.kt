package network.bisq.mobile.presentation.community

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.presentation.main.MainPresenter
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
        shipped: Set<CommunitySegment> = emptySet(),
        devForced: Set<CommunitySegment> = emptySet(),
        requiredFeatures: Map<CommunitySegment, Feature> = emptyMap(),
    ): CommunityHubPresenter {
        val hubService =
            CommunityHubService(
                backendCapabilitiesService =
                    object : BackendCapabilitiesService {
                        override val capabilities: StateFlow<BackendCapabilities> = capabilitiesFlow
                    },
                shippedSegments = shipped,
                devForcedSegments = devForced,
                requiredFeatures = requiredFeatures,
                dispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        val presenter = CommunityHubPresenter(mainPresenter, hubService)
        presenter.onViewAttached()
        advanceUntilIdle()
        return presenter
    }

    @Test
    fun `live segments render in declaration order with the first selected`() =
        runTest {
            val presenter =
                createAttachedPresenter(devForced = setOf(CommunitySegment.MESSAGES, CommunitySegment.DISCUSSIONS))

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
                createAttachedPresenter(devForced = setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.MESSAGES))

            presenter.onAction(CommunityHubUiAction.OnSegmentSelect(CommunitySegment.MESSAGES))

            assertEquals(CommunitySegment.MESSAGES, presenter.uiState.value.selectedSegment)
        }

    @Test
    fun `selecting a segment that is not live is ignored`() =
        runTest {
            val presenter = createAttachedPresenter(devForced = setOf(CommunitySegment.DISCUSSIONS))

            presenter.onAction(CommunityHubUiAction.OnSegmentSelect(CommunitySegment.CONTACTS))

            assertEquals(CommunitySegment.DISCUSSIONS, presenter.uiState.value.selectedSegment)
        }

    @Test
    fun `selection falls back to the first live segment when the selected one goes away`() =
        runTest {
            val presenter =
                createAttachedPresenter(
                    shipped = setOf(CommunitySegment.DISCUSSIONS),
                    devForced = setOf(CommunitySegment.MESSAGES),
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

    @Test
    fun `support quick access action is a safe no-op for now`() =
        runTest {
            val presenter = createAttachedPresenter(devForced = setOf(CommunitySegment.DISCUSSIONS))

            presenter.onAction(CommunityHubUiAction.OnOpenSupportChannel)
            advanceUntilIdle()

            assertEquals(CommunitySegment.DISCUSSIONS, presenter.uiState.value.selectedSegment)
        }
}
