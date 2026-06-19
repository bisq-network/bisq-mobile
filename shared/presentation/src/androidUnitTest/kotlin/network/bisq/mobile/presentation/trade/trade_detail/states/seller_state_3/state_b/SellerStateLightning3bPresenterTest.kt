package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_b

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.TradeStatePresenterTestSupport
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SellerStateLightning3bPresenterTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)

    @BeforeTest
    fun setUp() = TradeStatePresenterTestSupport.setUp(testDispatcher)

    @AfterTest
    fun tearDown() = TradeStatePresenterTestSupport.tearDown()

    @Test
    fun `rapid double-tap on skipWaiting triggers btcConfirmed only once`() =
        runTest(testDispatcher) {
            val presenter = SellerStateLightning3bPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.btcConfirmed() } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.skipWaiting()
            presenter.skipWaiting()
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.btcConfirmed() }
            assertFalse(presenter.isSkipWaitingEnabled.value)
        }

    @Test
    fun `skip waiting failure re-enables guard`() =
        runTest(testDispatcher) {
            val presenter = SellerStateLightning3bPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.btcConfirmed() } returns
                Result.failure(RuntimeException("failed"))

            presenter.skipWaiting()
            advanceUntilIdle()

            assertTrue(presenter.isSkipWaitingEnabled.value)
        }
}
