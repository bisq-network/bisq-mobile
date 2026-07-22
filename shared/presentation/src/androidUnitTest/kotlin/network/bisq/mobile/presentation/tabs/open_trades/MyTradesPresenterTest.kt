package network.bisq.mobile.presentation.tabs.open_trades

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.my_trades.MyTradesPresenter
import network.bisq.mobile.presentation.tabs.my_trades.MyTradesUiAction
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyTradesPresenterTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val navigationManager: NavigationManager = mockk(relaxed = true)
    private val globalUiManager by lazy { GlobalUiManager(testDispatcher) }

    private val capabilitiesFlow = MutableStateFlow(BackendCapabilities.UNAVAILABLE)
    private val backendCapabilitiesService: BackendCapabilitiesService =
        mockk<BackendCapabilitiesService>(relaxed = true).also {
            every { it.capabilities } returns capabilitiesFlow
        }

    private val testModule =
        module {
            single { CoroutineExceptionHandlerSetup() }
            factory<CoroutineJobsManager> {
                DefaultCoroutineJobsManager().apply {
                    get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                }
            }
            single<NavigationManager> { navigationManager }
            single<GlobalUiManager> { globalUiManager }
        }

    private lateinit var presenter: MyTradesPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(testModule) }
        I18nSupport.initialize("en")
        GenericErrorHandler.clearGenericError()
        every { mainPresenter.languageCode } returns MutableStateFlow("en")
        presenter = MyTradesPresenter(mainPresenter, backendCapabilitiesService)
        presenter.onViewAttached()
    }

    @AfterTest
    fun tearDown() {
        presenter.onViewUnattaching()
        stopKoin()
        Dispatchers.resetMain()
        GenericErrorHandler.clearGenericError()
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
