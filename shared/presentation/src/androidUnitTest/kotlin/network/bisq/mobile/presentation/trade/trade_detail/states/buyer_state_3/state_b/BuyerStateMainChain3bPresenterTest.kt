package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_3.state_b

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.explorer.ExplorerResult
import network.bisq.mobile.data.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class BuyerStateMainChain3bPresenterTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val explorerServiceFacade: ExplorerServiceFacade = mockk(relaxed = true)

    // Empty, as when the trade screen builds its presenters before initialize() selects the trade.
    private val selectedTrade = MutableStateFlow<TradeItemPresentationModel?>(null)

    override fun onKoinReady() {
        every { tradesServiceFacade.selectedTrade } returns selectedTrade
        coEvery { explorerServiceFacade.getSelectedBlockExplorer() } returns Result.success("mempool")
    }

    @Test
    fun `complete trade targets the attached trade, not a later selection`() =
        runTest {
            val presenter = BuyerStateMainChain3bPresenter(mainPresenter, tradesServiceFacade, explorerServiceFacade)
            coEvery { tradesServiceFacade.btcConfirmed(any()) } returns Result.success(Unit)
            selectedTrade.value = tradeWithId("trade-1")
            presenter.onViewAttached()

            presenter.onCompleteTrade()
            // Before the launched action runs: a deep link or notification switched the selection.
            selectedTrade.value = tradeWithId("trade-2")
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.btcConfirmed("trade-1") }
            coVerify(exactly = 0) { tradesServiceFacade.btcConfirmed("trade-2") }
        }

    @Test
    fun `the tx is looked up when the presenter is built before the trade is selected`() =
        runTest {
            // A failed lookup: a pending one would poll again every 20 s under advanceUntilIdle.
            coEvery { explorerServiceFacade.requestTx(any(), any()) } returns ExplorerResult(error = "e" to "m")
            val presenter = BuyerStateMainChain3bPresenter(mainPresenter, tradesServiceFacade, explorerServiceFacade)
            selectedTrade.value = tradeWithTx("trade-1", "txid", "address", 1000)

            presenter.onViewAttached()
            advanceUntilIdle()

            coVerify(exactly = 1) { explorerServiceFacade.requestTx("txid", "address") }
        }

    @Test
    fun `a tx amount from the previously attached trade is not compared with the next one`() =
        runTest {
            coEvery { explorerServiceFacade.requestTx("tx1", "a1") } returns
                ExplorerResult(isConfirmed = true, outputValues = listOf(999L), error = null)
            coEvery { explorerServiceFacade.requestTx("tx2", "a2") } coAnswers { awaitCancellation() }
            coEvery { tradesServiceFacade.btcConfirmed(any()) } returns Result.success(Unit)
            val presenter = BuyerStateMainChain3bPresenter(mainPresenter, tradesServiceFacade, explorerServiceFacade)
            selectedTrade.value = tradeWithTx("trade-1", "tx1", "a1", 999)
            presenter.onViewAttached()
            advanceUntilIdle()
            presenter.onViewUnattaching()

            // Trade-2's own lookup is still pending when complete is tapped.
            selectedTrade.value = tradeWithTx("trade-2", "tx2", "a2", 1000)
            presenter.onViewAttached()
            runCurrent()
            presenter.onCtaClick()
            runCurrent()

            assertNull(presenter.amountNotMatchingDialogText.value)
            coVerify(exactly = 1) { tradesServiceFacade.btcConfirmed("trade-2") }
            presenter.onViewUnattaching()
        }

    @Test
    fun `the mempool re-poll looks up the attached trade after the selection changes`() =
        runTest {
            // In the mempool, so it polls once; confirmed on the poll, so it stops.
            coEvery { explorerServiceFacade.requestTx(any(), any()) } returnsMany
                listOf(
                    ExplorerResult(isConfirmed = false, outputValues = listOf(1000L), error = null),
                    ExplorerResult(isConfirmed = true, outputValues = listOf(1000L), error = null),
                )
            val presenter = BuyerStateMainChain3bPresenter(mainPresenter, tradesServiceFacade, explorerServiceFacade)
            selectedTrade.value = tradeWithTx("trade-1", "tx1", "a1", 1000)
            presenter.onViewAttached()
            runCurrent()

            selectedTrade.value = tradeWithTx("trade-2", "tx2", "a2", 1000)
            advanceTimeBy(20_001)
            runCurrent()

            coVerify(exactly = 2) { explorerServiceFacade.requestTx("tx1", "a1") }
            coVerify(exactly = 0) { explorerServiceFacade.requestTx("tx2", any()) }
        }

    private fun tradeWithId(id: String): TradeItemPresentationModel =
        mockk(relaxed = true) {
            every { tradeId } returns id
            // No tx yet, so attaching looks nothing up.
            every { bisqEasyTradeModel.paymentProof } returns MutableStateFlow<String?>(null)
            every { bisqEasyTradeModel.bitcoinPaymentData } returns MutableStateFlow<String?>(null)
        }

    private fun tradeWithTx(
        id: String,
        txId: String,
        address: String,
        amount: Long,
    ): TradeItemPresentationModel =
        tradeWithId(id).apply {
            every { bisqEasyTradeModel.paymentProof } returns MutableStateFlow<String?>(txId)
            every { bisqEasyTradeModel.bitcoinPaymentData } returns MutableStateFlow<String?>(address)
            every { bisqEasyTradeModel.contract.baseSideAmount } returns amount
        }
}
