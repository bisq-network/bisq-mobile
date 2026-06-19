package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_a

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
class SellerState3aPresenterTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)

    @BeforeTest
    fun setUp() = TradeStatePresenterTestSupport.setUp(testDispatcher)

    @AfterTest
    fun tearDown() = TradeStatePresenterTestSupport.tearDown()

    @Test
    fun `rapid double-tap on confirmSend triggers sellerConfirmBtcSent only once`() =
        runTest(testDispatcher) {
            val presenter = SellerState3aPresenter(mainPresenter, tradesServiceFacade)
            presenter.onPaymentProofInput("tx-id-123", isValid = true)
            coEvery { tradesServiceFacade.sellerConfirmBtcSent(any()) } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.confirmSend()
            presenter.confirmSend()
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.sellerConfirmBtcSent("tx-id-123") }
            assertFalse(presenter.isConfirmSendEnabled.value)
        }

    @Test
    fun `confirm send failure re-enables confirm guard`() =
        runTest(testDispatcher) {
            val presenter = SellerState3aPresenter(mainPresenter, tradesServiceFacade)
            presenter.onPaymentProofInput("tx-id-123", isValid = true)
            coEvery { tradesServiceFacade.sellerConfirmBtcSent(any()) } returns
                Result.failure(RuntimeException("confirm failed"))

            presenter.confirmSend()
            advanceUntilIdle()

            assertTrue(presenter.isConfirmSendEnabled.value)
        }
}
