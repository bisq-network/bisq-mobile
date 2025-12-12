package network.bisq.mobile.presentation.trade.open_trade

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.trade.trade_detail.InterruptedTradePresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class InterruptedTradePresenterTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    // Mocks
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val mediationServiceFacade: MediationServiceFacade = mockk(relaxed = true)
    private val tradeReadStateRepository: TradeReadStateRepository = mockk(relaxed = true)
    private val navigationManager: NavigationManager = mockk(relaxed = true)

    // Use lazy initialization to inject test dispatcher into GlobalUiManager
    private val globalUiManager by lazy { GlobalUiManager(testDispatcher) }

    // Koin module for BasePresenter dependencies
    private val testKoinModule = module {
        single { CoroutineExceptionHandlerSetup() }
        factory<CoroutineJobsManager> {
            DefaultCoroutineJobsManager().apply {
                get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
            }
        }
        single<NavigationManager> { navigationManager }
        single<GlobalUiManager> { globalUiManager }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(testKoinModule) }
        I18nSupport.initialize("en")
        GenericErrorHandler.clearGenericError()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        GenericErrorHandler.clearGenericError()
    }

    @Test
    fun onCloseTrade_success_clearsReadState_navigatesBack_and_hidesLoading() = runTest {
        // Given
        val tradeItem = mockk<TradeItemPresentationModel>(relaxed = true)
        every { tradeItem.tradeId } returns "t-1"
        val selectedFlow = MutableStateFlow<TradeItemPresentationModel?>(tradeItem)
        every { tradesServiceFacade.selectedTrade } returns selectedFlow
        coEvery { tradesServiceFacade.closeTrade() } returns Result.success(Unit)

        val presenter = InterruptedTradePresenter(
            mainPresenter,
            tradesServiceFacade,
            mediationServiceFacade,
            tradeReadStateRepository
        )

        // When
        presenter.onCloseTrade()

        // Then: verify closeTrade invoked
        coVerify(timeout = 500) { tradesServiceFacade.closeTrade() }
        // Then: clears read state
        coVerify(timeout = 500) { tradeReadStateRepository.clearId("t-1") }
        // Then: navigates back
        verify(timeout = 500) { navigationManager.navigateBack(any()) }
        // And loading hidden
        waitUntil(timeoutMs = 1000) { globalUiManager.showLoadingDialog.value == false }
        assertFalse(globalUiManager.showLoadingDialog.value)
    }

    @Test
    fun onCloseTrade_failure_showsError_doesNotNavigate_and_hidesLoading() = runTest {
        // Given
        val tradeItem = mockk<TradeItemPresentationModel>(relaxed = true)
        every { tradeItem.tradeId } returns "t-2"
        val selectedFlow = MutableStateFlow<TradeItemPresentationModel?>(tradeItem)
        every { tradesServiceFacade.selectedTrade } returns selectedFlow
        coEvery { tradesServiceFacade.closeTrade() } returns Result.failure(RuntimeException("boom"))

        val presenter = InterruptedTradePresenter(
            mainPresenter,
            tradesServiceFacade,
            mediationServiceFacade,
            tradeReadStateRepository
        )

        // When
        presenter.onCloseTrade()

        // Then: verify closeTrade invoked
        coVerify(timeout = 500) { tradesServiceFacade.closeTrade() }
        // Should NOT clear read state
        coVerify(timeout = 300, exactly = 0) { tradeReadStateRepository.clearId(any()) }
        // Should NOT navigate back
        verify(timeout = 300, exactly = 0) { navigationManager.navigateBack(any()) }
        // Loading hidden
        waitUntil(timeoutMs = 1000) { globalUiManager.showLoadingDialog.value == false }
        assertFalse(globalUiManager.showLoadingDialog.value)
        // Error shown
        waitUntil(timeoutMs = 500) { GenericErrorHandler.genericErrorMessage.value != null }
        assertEquals(
            "Failed to close trade: boom",
            GenericErrorHandler.genericErrorMessage.value
        )
    }

    @Test
    fun onCloseTrade_success_but_clearReadState_throws_showsError_and_still_navigates() = runTest {
        // Given
        val tradeItem = mockk<TradeItemPresentationModel>(relaxed = true)
        every { tradeItem.tradeId } returns "t-3"
        val selectedFlow = MutableStateFlow<TradeItemPresentationModel?>(tradeItem)
        every { tradesServiceFacade.selectedTrade } returns selectedFlow
        coEvery { tradesServiceFacade.closeTrade() } returns Result.success(Unit)
        coEvery { tradeReadStateRepository.clearId("t-3") } throws IllegalStateException("fail-clear")

        val presenter = InterruptedTradePresenter(
            mainPresenter,
            tradesServiceFacade,
            mediationServiceFacade,
            tradeReadStateRepository
        )

        // When
        presenter.onCloseTrade()

        // Then: navigates back despite clearId failure
        verify(timeout = 500) { navigationManager.navigateBack(any()) }
        // Error was shown for clearReadState failure
        waitUntil(timeoutMs = 500) { GenericErrorHandler.genericErrorMessage.value?.contains("Failed to update read state") == true }
        // Loading hidden
        waitUntil(timeoutMs = 1000) { globalUiManager.showLoadingDialog.value == false }
        assertFalse(globalUiManager.showLoadingDialog.value)
    }

    // Helper: simple polling wait
    private suspend fun waitUntil(timeoutMs: Long, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) break
            delay(10)
        }
    }
}

