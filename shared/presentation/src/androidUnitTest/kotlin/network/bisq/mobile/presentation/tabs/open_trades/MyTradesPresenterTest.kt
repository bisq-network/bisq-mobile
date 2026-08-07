package network.bisq.mobile.presentation.tabs.open_trades

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.my_trades.MyTradesPresenter
import network.bisq.mobile.presentation.tabs.my_trades.MyTradesUiAction
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyTradesPresenterTest : PresentationKoinTestBase() {
    override val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val mainPresenter: MainPresenter = mockk(relaxed = true)

    private val capabilitiesFlow = MutableStateFlow(BackendCapabilities.UNAVAILABLE)
    private val backendCapabilitiesService: BackendCapabilitiesService =
        mockk<BackendCapabilitiesService>(relaxed = true).also {
            every { it.capabilities } returns capabilitiesFlow
        }

    private lateinit var presenter: MyTradesPresenter

    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    override fun onKoinReady() {
        GenericErrorHandler.clearGenericError()
        presenter = MyTradesPresenter(mainPresenter, backendCapabilitiesService)
        presenter.onViewAttached()
    }

    override fun onTearDown() {
        try {
            presenter.onViewUnattaching()
            GenericErrorHandler.clearGenericError()
        } finally {
            super.onTearDown()
        }
    }

    @Test
    fun `initial state selectedTab is 0`() {
        assertEquals(0, presenter.uiState.value.selectedTab)
    }

    @Test
    fun `setInitialTab with valid index updates selectedTab when history available`() {
        capabilitiesFlow.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))
        presenter.setInitialTab(1)
        assertEquals(1, presenter.uiState.value.selectedTab)
    }

    @Test
    fun `setInitialTab with index above LAST_TAB clamps to LAST_TAB when history available`() {
        capabilitiesFlow.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))
        presenter.setInitialTab(99)
        assertEquals(1, presenter.uiState.value.selectedTab)
    }

    @Test
    fun `setInitialTab clamps to 0 when history is unavailable`() {
        // Default capabilities: history unavailable → maxIndex collapses to 0.
        presenter.setInitialTab(1)
        assertEquals(0, presenter.uiState.value.selectedTab)
        presenter.setInitialTab(99)
        assertEquals(0, presenter.uiState.value.selectedTab)
    }

    @Test
    fun `setInitialTab with negative index clamps to 0`() {
        presenter.setInitialTab(-1)
        assertEquals(0, presenter.uiState.value.selectedTab)
    }

    @Test
    fun `OnSelectTab action above LAST_TAB clamps to LAST_TAB when history available`() {
        capabilitiesFlow.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))
        presenter.onAction(MyTradesUiAction.OnSelectTab(99))
        assertEquals(1, presenter.uiState.value.selectedTab)
    }

    @Test
    fun `OnSelectTab action with valid index updates selectedTab when history available`() {
        capabilitiesFlow.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))
        presenter.onAction(MyTradesUiAction.OnSelectTab(1))
        assertEquals(1, presenter.uiState.value.selectedTab)
    }

    @Test
    fun `OnSelectTab clamps to 0 when history is unavailable`() {
        presenter.onAction(MyTradesUiAction.OnSelectTab(1))
        assertEquals(0, presenter.uiState.value.selectedTab)
        presenter.onAction(MyTradesUiAction.OnSelectTab(99))
        assertEquals(0, presenter.uiState.value.selectedTab)
    }

    @Test
    fun `showHistoryTab reflects hasClosedTradesApi false by default`() {
        assertFalse(presenter.showHistoryTab.value)
    }

    @Test
    fun `showHistoryTab reflects hasClosedTradesApi true when capability enabled`() =
        runTest {
            capabilitiesFlow.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))
            assertTrue(presenter.showHistoryTab.value)
        }

    @Test
    fun `when showHistoryTab flips false while on history tab, presenter clamps to 0`() =
        runTest {
            // Enable history and navigate to tab 1
            capabilitiesFlow.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))
            presenter.setInitialTab(1)
            assertEquals(1, presenter.uiState.value.selectedTab)

            // Capability goes away — presenter should clamp back to 0
            capabilitiesFlow.value = BackendCapabilities()
            assertEquals(0, presenter.uiState.value.selectedTab)
        }

    @Test
    fun `when showHistoryTab flips false while on open tab, selectedTab stays at 0`() =
        runTest {
            capabilitiesFlow.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))
            presenter.setInitialTab(0)
            capabilitiesFlow.value = BackendCapabilities()
            assertEquals(0, presenter.uiState.value.selectedTab)
        }
}
