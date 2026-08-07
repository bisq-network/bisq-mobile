package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_3.state_b

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BuyerStateLightning3bPresenterTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)

    @Test
    fun `rapid double-tap on onCompleteTrade triggers btcConfirmed only once`() =
        runTest {
            val presenter = BuyerStateLightning3bPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.btcConfirmed() } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.onCompleteTrade()
            presenter.onCompleteTrade()
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.btcConfirmed() }
            assertFalse(presenter.isCompleteTradeEnabled.value)
        }

    @Test
    fun `complete trade failure re-enables guard`() =
        runTest {
            val presenter = BuyerStateLightning3bPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.btcConfirmed() } returns
                Result.failure(RuntimeException("failed"))

            presenter.onCompleteTrade()
            advanceUntilIdle()

            assertTrue(presenter.isCompleteTradeEnabled.value)
        }

    @Test
    fun `complete trade exception re-enables guard`() =
        runTest {
            val presenter = BuyerStateLightning3bPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.btcConfirmed() } throws RuntimeException("failed")

            presenter.onCompleteTrade()
            advanceUntilIdle()

            assertTrue(presenter.isCompleteTradeEnabled.value)
        }
}
