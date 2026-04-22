package network.bisq.mobile.presentation.trade.trade_detail.states.common

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
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.share.ShareFileService
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
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
class State4PresenterTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val tradeReadStateRepository: TradeReadStateRepository = mockk(relaxed = true)
    private val shareFileService: ShareFileService = mockk(relaxed = true)
    private val navigationManager: NavigationManager = mockk(relaxed = true)

    private val globalUiManager by lazy { GlobalUiManager(testDispatcher) }

    private val testKoinModule =
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

    private fun createPresenter(selectedTrade: MutableStateFlow<TradeItemPresentationModel?>): State4Presenter {
        every { tradesServiceFacade.selectedTrade } returns selectedTrade
        return TestState4Presenter(
            mainPresenter,
            tradesServiceFacade,
            tradeReadStateRepository,
            shareFileService,
        )
    }

    @Test
    fun when_selectedTrade_emits_then_uiState_has_trade_and_role_labels() =
        runTest {
            val trade = tradeForTests("full-id", "shorty")
            val selected = MutableStateFlow<TradeItemPresentationModel?>(null)
            val presenter = createPresenter(selected)

            presenter.onViewAttached()
            selected.value = trade

            assertEquals(trade, presenter.uiState.value.trade)
            assertEquals(TestState4Presenter.DIRECTION, presenter.uiState.value.myDirectionLabel)
            assertEquals(TestState4Presenter.OUTCOME, presenter.uiState.value.myOutcomeLabel)
        }

    @Test
    fun onCloseTradeClick_sets_showCloseTradeDialog_true() =
        runTest {
            val selected = MutableStateFlow<TradeItemPresentationModel?>(null)
            val presenter = createPresenter(selected)
            presenter.onViewAttached()

            presenter.onAction(State4UiAction.OnCloseTradeClick)

            assertTrue(presenter.uiState.value.showCloseTradeDialog)
        }

    @Test
    fun onDismissCloseTrade_sets_showCloseTradeDialog_false() =
        runTest {
            val selected = MutableStateFlow<TradeItemPresentationModel?>(null)
            val presenter = createPresenter(selected)
            presenter.onViewAttached()
            presenter.onAction(State4UiAction.OnCloseTradeClick)
            assertTrue(presenter.uiState.value.showCloseTradeDialog)

            presenter.onAction(State4UiAction.OnDismissCloseTrade)

            assertFalse(presenter.uiState.value.showCloseTradeDialog)
        }

    @Test
    fun onConfirmCloseTrade_success_clearsReadState_navigatesBack_and_closes_dialog() =
        runTest {
            val trade = tradeForTests("t-ok", "s1")
            val selected = MutableStateFlow<TradeItemPresentationModel?>(trade)
            val presenter = createPresenter(selected)
            presenter.onViewAttached()
            presenter.onAction(State4UiAction.OnCloseTradeClick)
            coEvery { tradesServiceFacade.closeTrade() } returns Result.success(Unit)

            presenter.onAction(State4UiAction.OnConfirmCloseTrade)

            coVerify(timeout = 500) { tradesServiceFacade.closeTrade() }
            coVerify(timeout = 500) { tradeReadStateRepository.clearId("t-ok") }
            verify(timeout = 500) { navigationManager.navigateBack(any()) }
            assertFalse(presenter.uiState.value.showCloseTradeDialog)
            waitUntil(timeoutMs = 1000) { globalUiManager.showLoadingDialog.value == false }
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun onConfirmCloseTrade_failure_shows_error_and_closes_dialog() =
        runTest {
            val trade = tradeForTests("t-fail", "s2")
            val selected = MutableStateFlow<TradeItemPresentationModel?>(trade)
            val presenter = createPresenter(selected)
            presenter.onViewAttached()
            presenter.onAction(State4UiAction.OnCloseTradeClick)
            coEvery { tradesServiceFacade.closeTrade() } returns Result.failure(RuntimeException("boom"))

            presenter.onAction(State4UiAction.OnConfirmCloseTrade)

            coVerify(timeout = 500) { tradesServiceFacade.closeTrade() }
            coVerify(timeout = 300, exactly = 0) { tradeReadStateRepository.clearId(any()) }
            verify(timeout = 300, exactly = 0) { navigationManager.navigateBack(any()) }
            assertFalse(presenter.uiState.value.showCloseTradeDialog)
            waitUntil(timeoutMs = 500) { GenericErrorHandler.genericErrorMessage.value != null }
            assertEquals("boom", GenericErrorHandler.genericErrorMessage.value)
        }

    @Test
    fun onConfirmCloseTrade_when_trade_missing_sets_error_and_closes_dialog() =
        runTest {
            val selected = MutableStateFlow<TradeItemPresentationModel?>(null)
            val presenter = createPresenter(selected)
            presenter.onViewAttached()
            presenter.onAction(State4UiAction.OnCloseTradeClick)
            assertTrue(presenter.uiState.value.showCloseTradeDialog)

            presenter.onAction(State4UiAction.OnConfirmCloseTrade)

            coVerify(timeout = 300, exactly = 0) { tradesServiceFacade.closeTrade() }
            assertFalse(presenter.uiState.value.showCloseTradeDialog)
            waitUntil(timeoutMs = 500) { GenericErrorHandler.genericErrorMessage.value != null }
            assertEquals("No trade selected for closure", GenericErrorHandler.genericErrorMessage.value)
        }

    @Test
    fun onExportTradeClick_calls_share_with_csv_file_name() =
        runTest {
            val trade = tradeForTests("t-exp", "short99")
            every { trade.formattedBaseAmount } returns "0.01 BTC"
            val selected = MutableStateFlow<TradeItemPresentationModel?>(trade)
            val presenter = createPresenter(selected)
            presenter.onViewAttached()
            coEvery { shareFileService.shareUtf8TextFile(any(), any()) } returns Result.success(Unit)

            presenter.onAction(State4UiAction.OnExportTradeClick)

            coVerify(timeout = 500) {
                shareFileService.shareUtf8TextFile(any(), "BisqEasy-trade-short99.csv")
            }
        }

    @Test
    fun onExportTradeClick_when_trade_missing_shows_error() =
        runTest {
            val selected = MutableStateFlow<TradeItemPresentationModel?>(null)
            val presenter = createPresenter(selected)
            presenter.onViewAttached()

            presenter.onAction(State4UiAction.OnExportTradeClick)

            coVerify(timeout = 300, exactly = 0) { shareFileService.shareUtf8TextFile(any(), any()) }
            waitUntil(timeoutMs = 500) { GenericErrorHandler.genericErrorMessage.value != null }
            assertEquals("No trade selected for export", GenericErrorHandler.genericErrorMessage.value)
        }

    @Test
    fun onExportTradeClick_when_share_fails_shows_error() =
        runTest {
            val trade = tradeForTests("t-bad-share", "sx")
            val selected = MutableStateFlow<TradeItemPresentationModel?>(trade)
            val presenter = createPresenter(selected)
            presenter.onViewAttached()
            coEvery { shareFileService.shareUtf8TextFile(any(), any()) } returns
                Result.failure(RuntimeException("share denied"))

            presenter.onAction(State4UiAction.OnExportTradeClick)

            coVerify(timeout = 5000) { shareFileService.shareUtf8TextFile(any(), any()) }
            assertEquals("share denied", GenericErrorHandler.genericErrorMessage.value)
        }

    @Test
    fun onViewUnattaching_resets_showCloseTradeDialog() =
        runTest {
            val selected = MutableStateFlow<TradeItemPresentationModel?>(null)
            val presenter = createPresenter(selected)
            presenter.onViewAttached()
            presenter.onAction(State4UiAction.OnCloseTradeClick)
            assertTrue(presenter.uiState.value.showCloseTradeDialog)

            presenter.onViewUnattaching()

            assertFalse(presenter.uiState.value.showCloseTradeDialog)
        }

    private fun tradeForTests(
        tradeId: String,
        shortTradeId: String,
    ): TradeItemPresentationModel {
        val trade = mockk<TradeItemPresentationModel>(relaxed = true)
        every { trade.tradeId } returns tradeId
        every { trade.shortTradeId } returns shortTradeId
        every { trade.baseAmount } returns 100_000_000L
        every { trade.quoteAmount } returns 10_000L
        every { trade.quoteCurrencyCode } returns "USD"
        every { trade.paymentMethodCsvDisplayString } returns "SEPA"
        val tm = mockk<BisqEasyTradeModel>(relaxed = true)
        every { trade.bisqEasyTradeModel } returns tm
        every { tm.paymentProof } returns MutableStateFlow(null)
        every { tm.bitcoinPaymentData } returns MutableStateFlow(null)
        return trade
    }

    private suspend fun waitUntil(
        timeoutMs: Long,
        condition: () -> Boolean,
    ) {
        withTimeout(timeoutMs) {
            while (!condition()) {
                delay(10)
            }
        }
    }

    private class TestState4Presenter(
        mainPresenter: MainPresenter,
        tradesServiceFacade: TradesServiceFacade,
        tradeReadStateRepository: TradeReadStateRepository,
        shareFileService: ShareFileService,
    ) : State4Presenter(mainPresenter, tradesServiceFacade, tradeReadStateRepository, shareFileService) {
        override fun resolveMyDirectionLabel(): String = DIRECTION

        override fun resolveMyOutcomeLabel(): String = OUTCOME

        companion object {
            const val DIRECTION = "dir-label"
            const val OUTCOME = "out-label"
        }
    }
}
