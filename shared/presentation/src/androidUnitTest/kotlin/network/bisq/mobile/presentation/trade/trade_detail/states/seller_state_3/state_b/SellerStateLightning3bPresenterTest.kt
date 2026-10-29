package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_b

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SellerStateLightning3bPresenterTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val selectedTrade = MutableStateFlow<TradeItemPresentationModel?>(tradeWithId("trade-1"))

    override fun onKoinReady() {
        every { tradesServiceFacade.selectedTrade } returns selectedTrade
    }

    @Test
    fun `rapid double-tap on skipWaiting triggers btcConfirmed only once`() =
        runTest {
            val presenter = SellerStateLightning3bPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.btcConfirmed("trade-1") } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.skipWaiting()
            presenter.skipWaiting()
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.btcConfirmed("trade-1") }
            assertFalse(presenter.isSkipWaitingEnabled.value)
        }

    @Test
    fun `skip waiting failure re-enables guard`() =
        runTest {
            val presenter = SellerStateLightning3bPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.btcConfirmed("trade-1") } returns
                Result.failure(RuntimeException("failed"))

            presenter.skipWaiting()
            advanceUntilIdle()

            assertTrue(presenter.isSkipWaitingEnabled.value)
        }

    @Test
    fun `skip waiting targets the trade on screen when tapped, not the selection at resolution`() =
        runTest {
            val presenter = SellerStateLightning3bPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.btcConfirmed(any()) } returns Result.success(Unit)

            presenter.skipWaiting()
            // Before the launched action runs: a deep link or notification switched the selection.
            selectedTrade.value = tradeWithId("trade-2")
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.btcConfirmed("trade-1") }
            coVerify(exactly = 0) { tradesServiceFacade.btcConfirmed("trade-2") }
        }

    private fun tradeWithId(id: String): TradeItemPresentationModel = mockk(relaxed = true) { every { tradeId } returns id }
}
