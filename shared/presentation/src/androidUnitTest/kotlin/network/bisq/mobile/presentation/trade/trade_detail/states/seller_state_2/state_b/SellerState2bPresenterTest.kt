package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_2.state_b

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
class SellerState2bPresenterTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)

    @BeforeTest
    fun setUp() = TradeStatePresenterTestSupport.setUp(testDispatcher)

    @AfterTest
    fun tearDown() = TradeStatePresenterTestSupport.tearDown()

    @Test
    fun `rapid double-tap on onConfirmFiatReceipt triggers sellerConfirmFiatReceipt only once`() =
        runTest(testDispatcher) {
            val presenter = SellerState2bPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.sellerConfirmFiatReceipt() } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.onConfirmFiatReceipt()
            presenter.onConfirmFiatReceipt()
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.sellerConfirmFiatReceipt() }
            assertFalse(presenter.isConfirmFiatReceiptEnabled.value)
        }

    @Test
    fun `confirm fiat receipt failure re-enables guard`() =
        runTest(testDispatcher) {
            val presenter = SellerState2bPresenter(mainPresenter, tradesServiceFacade)
            coEvery { tradesServiceFacade.sellerConfirmFiatReceipt() } returns
                Result.failure(RuntimeException("failed"))

            presenter.onConfirmFiatReceipt()
            advanceUntilIdle()

            assertTrue(presenter.isConfirmFiatReceiptEnabled.value)
        }
}
