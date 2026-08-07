package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a

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
class BuyerState2aPresenterTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)

    @Test
    fun `rapid double-tap on onConfirmFiatSent triggers buyerConfirmFiatSent only once`() =
        runTest {
            val presenter = BuyerState2aPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.buyerConfirmFiatSent() } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.onConfirmFiatSent()
            presenter.onConfirmFiatSent()
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.buyerConfirmFiatSent() }
            assertFalse(presenter.isConfirmFiatSentEnabled.value)
        }

    @Test
    fun `confirm fiat sent failure re-enables guard`() =
        runTest {
            val presenter = BuyerState2aPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.buyerConfirmFiatSent() } returns
                Result.failure(RuntimeException("failed"))

            presenter.onConfirmFiatSent()
            advanceUntilIdle()

            assertTrue(presenter.isConfirmFiatSentEnabled.value)
        }
}
