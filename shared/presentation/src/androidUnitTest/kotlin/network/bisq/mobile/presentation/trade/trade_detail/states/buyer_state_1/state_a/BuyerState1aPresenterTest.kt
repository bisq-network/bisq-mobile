package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a

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
class BuyerState1aPresenterTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)

    @Test
    fun `rapid double-tap on sendBitcoinPaymentData triggers buyerSendBitcoinPaymentData only once`() =
        runTest {
            val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade)
            presenter.onBitcoinPaymentDataInput("bc1qexampleaddress", isValid = true)
            coEvery { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.sendBitcoinPaymentData()
            presenter.sendBitcoinPaymentData()
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) }
            assertFalse(presenter.isSendBitcoinPaymentDataEnabled.value)
        }

    @Test
    fun `over-protocol-length address is hard-blocked without offering proceed anyway`() =
        runTest {
            val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade)
            // 63 chars: one over bisq2's BitcoinAddressValidation.MAX_LENGTH — the peer's
            // BisqEasyBtcAddressMessageHandler would reject it unconditionally.
            presenter.onBitcoinPaymentDataInput("bc1q" + "x".repeat(59), isValid = false)

            presenter.onSendBitcoinPaymentDataClick()
            advanceUntilIdle()

            coVerify(exactly = 0) { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) }
            assertFalse(presenter.showInvalidAddressDialog.value)
        }

    @Test
    fun `over-protocol-length address is blocked even on the proceed-anyway path`() =
        runTest {
            val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade)
            presenter.onBitcoinPaymentDataInput("bc1q" + "x".repeat(59), isValid = false)

            presenter.sendBitcoinPaymentData()
            advanceUntilIdle()

            coVerify(exactly = 0) { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) }
        }

    @Test
    fun `invalid address within protocol length still offers the proceed-anyway dialog`() =
        runTest {
            val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade)
            presenter.onBitcoinPaymentDataInput("not-a-real-address", isValid = false)

            presenter.onSendBitcoinPaymentDataClick()
            advanceUntilIdle()

            coVerify(exactly = 0) { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) }
            assertTrue(presenter.showInvalidAddressDialog.value)
        }

    @Test
    fun `failure path re-enables send button for retry`() =
        runTest {
            val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade)
            presenter.onBitcoinPaymentDataInput("bc1qexampleaddress", isValid = true)
            coEvery { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) } returns
                Result.failure(RuntimeException("network error"))

            presenter.sendBitcoinPaymentData()
            advanceUntilIdle()

            assertTrue(presenter.isSendBitcoinPaymentDataEnabled.value)
        }
}
