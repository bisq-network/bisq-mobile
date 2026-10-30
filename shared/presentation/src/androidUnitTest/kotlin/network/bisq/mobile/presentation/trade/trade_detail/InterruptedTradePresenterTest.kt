package network.bisq.mobile.presentation.trade.trade_detail

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.data.model.TradeCancelReason
import network.bisq.mobile.data.model.TradeCancelReasonMap
import network.bisq.mobile.data.replicated.contract.RoleEnum
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.service.mediation.MediationServiceFacade
import network.bisq.mobile.data.service.offers.MediatorNotAvailableException
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.repository.TradeCancelReasonRepository
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InterruptedTradePresenterTest : PresentationKoinTestBase() {
    override val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val mediationServiceFacade: MediationServiceFacade = mockk(relaxed = true)
    private val tradeReadStateRepository: TradeReadStateRepository = mockk(relaxed = true)
    private val cancelReasons = MutableStateFlow(TradeCancelReasonMap())
    private val tradeCancelReasonRepository: TradeCancelReasonRepository =
        mockk(relaxed = true) { every { data } returns cancelReasons }

    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        GenericErrorHandler.clearGenericError()
    }

    override fun onTearDown() {
        try {
            GenericErrorHandler.clearGenericError()
        } finally {
            super.onTearDown()
        }
    }

    private fun createPresenter() =
        InterruptedTradePresenter(
            mainPresenter,
            tradesServiceFacade,
            mediationServiceFacade,
            tradeReadStateRepository,
            tradeCancelReasonRepository,
        )

    private fun selectTrade(tradeId: String): TradeItemPresentationModel {
        val tradeItem = mockk<TradeItemPresentationModel>(relaxed = true)
        every { tradeItem.tradeId } returns tradeId
        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(tradeItem)
        return tradeItem
    }

    @Test
    fun onCloseTrade_success_clearsReadState_navigatesBack_and_hidesLoading() =
        runTest {
            selectTrade("t-1")
            coEvery { tradesServiceFacade.closeTrade() } returns Result.success(Unit)

            createPresenter().onAction(InterruptedTradeUiAction.OnCloseTrade)

            coVerify(timeout = 500) { tradesServiceFacade.closeTrade() }
            coVerify(timeout = 500) { tradeReadStateRepository.clearId("t-1") }
            coVerify(timeout = 500) { tradeCancelReasonRepository.clearId("t-1") }
            verify(timeout = 500) { navigationManager.navigateBack(any()) }
            waitUntil(timeoutMs = 1000) { globalUiManager.showLoadingDialog.value == false }
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun onCloseTrade_failure_showsError_doesNotNavigate_and_hidesLoading() =
        runTest {
            selectTrade("t-2")
            coEvery { tradesServiceFacade.closeTrade() } returns Result.failure(RuntimeException("boom"))

            createPresenter().onAction(InterruptedTradeUiAction.OnCloseTrade)

            coVerify(timeout = 500) { tradesServiceFacade.closeTrade() }
            coVerify(timeout = 300, exactly = 0) { tradeReadStateRepository.clearId(any()) }
            coVerify(timeout = 300, exactly = 0) { tradeCancelReasonRepository.clearId(any()) }
            verify(timeout = 300, exactly = 0) { navigationManager.navigateBack(any()) }
            waitUntil(timeoutMs = 1000) { globalUiManager.showLoadingDialog.value == false }
            assertFalse(globalUiManager.showLoadingDialog.value)
            waitUntil(timeoutMs = 500) { GenericErrorHandler.genericErrorMessage.value != null }
            assertEquals(
                "Failed to close trade: boom",
                GenericErrorHandler.genericErrorMessage.value,
            )
        }

    @Test
    fun onCloseTrade_success_but_clearReadState_throws_showsError_and_still_navigates() =
        runTest {
            selectTrade("t-3")
            coEvery { tradesServiceFacade.closeTrade() } returns Result.success(Unit)
            coEvery { tradeReadStateRepository.clearId("t-3") } throws IllegalStateException("fail-clear")

            createPresenter().onAction(InterruptedTradeUiAction.OnCloseTrade)

            verify(timeout = 500) { navigationManager.navigateBack(any()) }
            waitUntil(timeoutMs = 500) { GenericErrorHandler.genericErrorMessage.value?.contains("Failed to update read state") == true }
            waitUntil(timeoutMs = 1000) { globalUiManager.showLoadingDialog.value == false }
            assertFalse(globalUiManager.showLoadingDialog.value)
        }

    @Test
    fun onCloseTrade_success_but_clearing_the_cancel_reason_throws_still_navigates() =
        runTest {
            selectTrade("t-6")
            coEvery { tradesServiceFacade.closeTrade() } returns Result.success(Unit)
            coEvery { tradeCancelReasonRepository.clearId("t-6") } throws IllegalStateException("fail-clear")

            createPresenter().onAction(InterruptedTradeUiAction.OnCloseTrade)

            coVerify(timeout = 500) { tradeReadStateRepository.clearId("t-6") }
            verify(timeout = 500) { navigationManager.navigateBack(any()) }
        }

    @Test
    fun onReportToMediator_success_shows_mediation_requested_dialog_until_dismissed() =
        runTest {
            val tradeItem = selectTrade("t-4")
            coEvery { mediationServiceFacade.reportToMediator(tradeItem) } returns Result.success(Unit)
            val presenter = createPresenter()

            presenter.onAction(InterruptedTradeUiAction.OnReportToMediator)

            coVerify { mediationServiceFacade.reportToMediator(tradeItem) }
            assertTrue(presenter.uiState.value.isMediationRequestedDialogVisible)

            presenter.onAction(InterruptedTradeUiAction.OnDismissMediationRequestedDialog)

            assertFalse(presenter.uiState.value.isMediationRequestedDialogVisible)
        }

    @Test
    fun onReportToMediator_without_mediator_shows_warning_until_dismissed() =
        runTest {
            val tradeItem = selectTrade("t-5")
            coEvery { mediationServiceFacade.reportToMediator(tradeItem) } returns Result.failure(MediatorNotAvailableException())
            val presenter = createPresenter()

            presenter.onAction(InterruptedTradeUiAction.OnReportToMediator)

            assertTrue(presenter.uiState.value.isNoMediatorAvailableWarningVisible)
            assertFalse(presenter.uiState.value.isMediationRequestedDialogVisible)

            presenter.onAction(InterruptedTradeUiAction.OnDismissNoMediatorAvailableWarning)

            assertFalse(presenter.uiState.value.isNoMediatorAvailableWarningVisible)
        }

    @Test
    fun `FAILED state shows an error message`() {
        assertErrorMessageVisible(BisqEasyTradeStateEnum.FAILED)
    }

    @Test
    fun `FAILED_AT_PEER state shows an error message`() {
        assertErrorMessageVisible(BisqEasyTradeStateEnum.FAILED_AT_PEER)
    }

    @Test
    fun `self cancelled trade shows the self text without the mediator button`() {
        val state = attachAndInterrupt(BisqEasyTradeStateEnum.CANCELLED, isMaker = true, initiator = RoleEnum.MAKER)

        assertEquals("bisqEasy.openTrades.cancelled.self".i18n(), state.interruptedTradeInfo)
        assertFalse(state.isReportToMediatorButtonVisible)
    }

    @Test
    fun `peer cancelled trade shows the peer text and the mediator button`() {
        val state = attachAndInterrupt(BisqEasyTradeStateEnum.PEER_CANCELLED, isMaker = false, initiator = RoleEnum.MAKER)

        assertEquals("bisqEasy.openTrades.cancelled.peer".i18n(), state.interruptedTradeInfo)
        assertTrue(state.isReportToMediatorButtonVisible)
    }

    @Test
    fun `self rejected trade shows the self text without the mediator button`() {
        val state = attachAndInterrupt(BisqEasyTradeStateEnum.REJECTED, isMaker = false, initiator = RoleEnum.TAKER)

        assertEquals("bisqEasy.openTrades.rejected.self".i18n(), state.interruptedTradeInfo)
        assertFalse(state.isReportToMediatorButtonVisible)
    }

    @Test
    fun `peer rejected trade shows the peer text without the mediator button`() {
        val state = attachAndInterrupt(BisqEasyTradeStateEnum.PEER_REJECTED, isMaker = true, initiator = RoleEnum.TAKER)

        assertEquals("bisqEasy.openTrades.rejected.peer".i18n(), state.interruptedTradeInfo)
        assertFalse(state.isReportToMediatorButtonVisible)
    }

    @Test
    fun `self cancelled trade with the banned account data reason explains it`() {
        cancelReasons.value = TradeCancelReasonMap(mapOf(TRADE_ID to TradeCancelReason.BANNED_ACCOUNT_DATA))

        val state = attachAndInterrupt(BisqEasyTradeStateEnum.CANCELLED, isMaker = false, initiator = RoleEnum.TAKER)

        assertEquals("mobile.bisqEasy.openTrades.cancelled.self.bannedAccountData".i18n(), state.interruptedTradeInfo)
        assertFalse(state.isReportToMediatorButtonVisible)
    }

    @Test
    fun `a banned account data reason saved for another trade is ignored`() {
        cancelReasons.value = TradeCancelReasonMap(mapOf("other-trade" to TradeCancelReason.BANNED_ACCOUNT_DATA))

        val state = attachAndInterrupt(BisqEasyTradeStateEnum.CANCELLED, isMaker = false, initiator = RoleEnum.TAKER)

        assertEquals("bisqEasy.openTrades.cancelled.self".i18n(), state.interruptedTradeInfo)
    }

    @Test
    fun `peer cancelled trade ignores a saved reason`() {
        cancelReasons.value = TradeCancelReasonMap(mapOf(TRADE_ID to TradeCancelReason.BANNED_ACCOUNT_DATA))

        val state = attachAndInterrupt(BisqEasyTradeStateEnum.PEER_CANCELLED, isMaker = false, initiator = RoleEnum.MAKER)

        assertEquals("bisqEasy.openTrades.cancelled.peer".i18n(), state.interruptedTradeInfo)
        assertTrue(state.isReportToMediatorButtonVisible)
    }

    @Test
    fun `rejected trade ignores a saved reason`() {
        cancelReasons.value = TradeCancelReasonMap(mapOf(TRADE_ID to TradeCancelReason.BANNED_ACCOUNT_DATA))

        val state = attachAndInterrupt(BisqEasyTradeStateEnum.REJECTED, isMaker = false, initiator = RoleEnum.TAKER)

        assertEquals("bisqEasy.openTrades.rejected.self".i18n(), state.interruptedTradeInfo)
    }

    @Test
    fun `a reason saved after the cancel shows up without reattaching`() =
        runTest {
            val harness = givenSelectedTrade(isMaker = false, initiator = RoleEnum.TAKER)
            val presenter = createPresenter()
            presenter.onViewAttached()
            harness.tradeStateFlow.value = BisqEasyTradeStateEnum.CANCELLED
            assertEquals("bisqEasy.openTrades.cancelled.self".i18n(), presenter.uiState.value.interruptedTradeInfo)

            cancelReasons.value = TradeCancelReasonMap(mapOf(TRADE_ID to TradeCancelReason.BANNED_ACCOUNT_DATA))

            assertEquals("mobile.bisqEasy.openTrades.cancelled.self.bannedAccountData".i18n(), presenter.uiState.value.interruptedTradeInfo)
            presenter.onViewUnattaching()
        }

    private fun givenSelectedTrade(
        isMaker: Boolean,
        initiator: RoleEnum,
    ): TradeDetailsHeaderTestHarness {
        val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
        val tradeItem = harness.selectedTrade.value!!
        val tradeModel = tradeItem.bisqEasyTradeModel
        every { tradeItem.tradeId } returns TRADE_ID
        every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade
        every { tradeModel.isMaker } returns isMaker
        every { tradeModel.interruptTradeInitiator } returns MutableStateFlow(initiator)
        return harness
    }

    private fun attachAndInterrupt(
        tradeState: BisqEasyTradeStateEnum,
        isMaker: Boolean,
        initiator: RoleEnum,
    ): InterruptedTradeUiState {
        lateinit var uiState: InterruptedTradeUiState
        runTest {
            val harness = givenSelectedTrade(isMaker, initiator)
            val presenter = createPresenter()

            presenter.onViewAttached()
            harness.tradeStateFlow.value = tradeState

            uiState = presenter.uiState.value
            assertNull(uiState.errorMessage)
            presenter.onViewUnattaching()
        }
        return uiState
    }

    private fun assertErrorMessageVisible(state: BisqEasyTradeStateEnum) =
        runTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            val tradeModel = harness.selectedTrade.value!!.bisqEasyTradeModel
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade
            every { tradeModel.errorMessage } returns MutableStateFlow("simulated")
            every { tradeModel.peersErrorMessage } returns MutableStateFlow("simulated")
            val presenter = createPresenter()

            presenter.onViewAttached()
            harness.tradeStateFlow.value = state

            assertNotNull(presenter.uiState.value.errorMessage)
            assertNull(presenter.uiState.value.interruptedTradeInfo)
            presenter.onViewUnattaching()
        }

    // Helper: simple polling wait
    private suspend fun waitUntil(
        timeoutMs: Long,
        condition: () -> Boolean,
    ) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) break
            delay(10)
        }
    }

    private companion object {
        const val TRADE_ID = "trade-1"
    }
}
