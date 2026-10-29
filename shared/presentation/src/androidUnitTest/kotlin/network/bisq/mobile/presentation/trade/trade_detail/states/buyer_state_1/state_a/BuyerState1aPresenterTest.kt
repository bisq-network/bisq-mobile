package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.test_utils.FakePayoutAddressPrepRepository
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BuyerState1aPresenterTest : PresentationKoinTestBase() {
    private companion object {
        const val PREFILLED_ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        const val INVOICE = "lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdq5xysxxatsyp3k7enxv4js"
    }

    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val payoutAddressPrepRepository = FakePayoutAddressPrepRepository()

    override fun onKoinReady() {
        I18nSupport.initialize("en")
    }

    @Test
    fun `rapid double-tap on sendBitcoinPaymentData triggers buyerSendBitcoinPaymentData only once`() =
        runTest {
            val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade, payoutAddressPrepRepository)
            presenter.onAction(BuyerState1aUiAction.OnPaymentDataInput("bc1qexampleaddress", isValid = true))
            coEvery { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.onAction(BuyerState1aUiAction.OnSendAnyway)
            presenter.onAction(BuyerState1aUiAction.OnSendAnyway)
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) }
            assertFalse(presenter.isSendBitcoinPaymentDataEnabled.value)
        }

    @Test
    fun `over-protocol-length address is hard-blocked without offering proceed anyway`() =
        runTest {
            val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade, payoutAddressPrepRepository)
            // 63 chars: one over bisq2's BitcoinAddressValidation.MAX_LENGTH — the peer's
            // BisqEasyBtcAddressMessageHandler would reject it unconditionally.
            presenter.onAction(BuyerState1aUiAction.OnPaymentDataInput("bc1q" + "x".repeat(59), isValid = false))

            presenter.onAction(BuyerState1aUiAction.OnSend)
            advanceUntilIdle()

            coVerify(exactly = 0) { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) }
            assertFalse(presenter.uiState.value.showInvalidAddressDialog)
        }

    @Test
    fun `over-protocol-length address is blocked even on the proceed-anyway path`() =
        runTest {
            val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade, payoutAddressPrepRepository)
            presenter.onAction(BuyerState1aUiAction.OnPaymentDataInput("bc1q" + "x".repeat(59), isValid = false))

            presenter.onAction(BuyerState1aUiAction.OnSendAnyway)
            advanceUntilIdle()

            coVerify(exactly = 0) { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) }
        }

    @Test
    fun `invalid address within protocol length still offers the proceed-anyway dialog`() =
        runTest {
            val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade, payoutAddressPrepRepository)
            presenter.onAction(BuyerState1aUiAction.OnPaymentDataInput("not-a-real-address", isValid = false))

            presenter.onAction(BuyerState1aUiAction.OnSend)
            advanceUntilIdle()

            coVerify(exactly = 0) { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) }
            assertTrue(presenter.uiState.value.showInvalidAddressDialog)
        }

    @Test
    fun `failure path re-enables send button for retry`() =
        runTest {
            val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade, payoutAddressPrepRepository)
            presenter.onAction(BuyerState1aUiAction.OnPaymentDataInput("bc1qexampleaddress", isValid = true))
            coEvery { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) } returns
                Result.failure(RuntimeException("network error"))

            presenter.onAction(BuyerState1aUiAction.OnSendAnyway)
            advanceUntilIdle()

            assertTrue(presenter.isSendBitcoinPaymentDataEnabled.value)
        }

    // ============== Scanned payment data ============================

    @Test
    fun `a scanned valid lightning invoice is sent without the invalid dialog`() =
        runTest {
            val presenter = attachWithTrade(prefill = null, settlementMethod = "LN")
            coEvery { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) } returns Result.success(Unit)

            presenter.onAction(BuyerState1aUiAction.OnBarcodeResult(INVOICE))
            presenter.onAction(BuyerState1aUiAction.OnSend)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.showInvalidAddressDialog)
            coVerify(exactly = 1) { tradesServiceFacade.buyerSendBitcoinPaymentData(INVOICE) }
        }

    @Test
    fun `a scanned upper case lightning uri is sent as the lower case invoice`() =
        runTest {
            val presenter = attachWithTrade(prefill = null, settlementMethod = "LN")
            coEvery { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) } returns Result.success(Unit)

            presenter.onAction(BuyerState1aUiAction.OnBarcodeResult("LIGHTNING:" + INVOICE.uppercase()))
            presenter.onAction(BuyerState1aUiAction.OnSend)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.showInvalidAddressDialog)
            coVerify(exactly = 1) { tradesServiceFacade.buyerSendBitcoinPaymentData(INVOICE) }
        }

    @Test
    fun `a scanned valid bitcoin uri is sent as its address without the invalid dialog`() =
        runTest {
            val presenter = attachWithTrade(prefill = null)
            coEvery { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) } returns Result.success(Unit)

            presenter.onAction(BuyerState1aUiAction.OnBarcodeResult("BITCOIN:" + PREFILLED_ADDRESS.uppercase() + "?amount=0.001"))
            presenter.onAction(BuyerState1aUiAction.OnSend)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.showInvalidAddressDialog)
            coVerify(exactly = 1) { tradesServiceFacade.buyerSendBitcoinPaymentData(PREFILLED_ADDRESS) }
        }

    @Test
    fun `a scanned invalid invoice still offers the proceed-anyway dialog`() =
        runTest {
            val presenter = attachWithTrade(prefill = null, settlementMethod = "LN")

            presenter.onAction(BuyerState1aUiAction.OnBarcodeResult("lnbc1notaninvoice"))
            presenter.onAction(BuyerState1aUiAction.OnSend)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.showInvalidAddressDialog)
            coVerify(exactly = 0) { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) }
        }

    // ============== Wizard pre-fill =================================

    @Test
    fun `a stored wizard address seeds the field and arms the confirmation gate`() =
        runTest {
            val presenter = attachWithTrade(prefill = PREFILLED_ADDRESS)
            coEvery { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) } returns Result.success(Unit)

            assertEquals(PREFILLED_ADDRESS, presenter.uiState.value.paymentData)
            assertTrue(presenter.uiState.value.wasPrefilled)
            assertFalse(presenter.uiState.value.hasConfirmedPrefill)

            // Unconfirmed pre-fill: the send tap is refused even though the address is valid.
            presenter.onAction(BuyerState1aUiAction.OnSend)
            advanceUntilIdle()
            coVerify(exactly = 0) { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) }

            presenter.onAction(BuyerState1aUiAction.OnConfirmPrefillChange(true))
            presenter.onAction(BuyerState1aUiAction.OnSend)
            advanceUntilIdle()
            coVerify(exactly = 1) { tradesServiceFacade.buyerSendBitcoinPaymentData(PREFILLED_ADDRESS) }
        }

    @Test
    fun `editing the seeded address lifts the gate without the checkbox`() =
        runTest {
            val presenter = attachWithTrade(prefill = PREFILLED_ADDRESS)
            coEvery { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) } returns Result.success(Unit)

            presenter.onAction(BuyerState1aUiAction.OnPaymentDataInput("bc1qexampleaddress", isValid = true))

            assertFalse(presenter.uiState.value.wasPrefilled)
            presenter.onAction(BuyerState1aUiAction.OnSend)
            advanceUntilIdle()
            coVerify(exactly = 1) { tradesServiceFacade.buyerSendBitcoinPaymentData("bc1qexampleaddress") }
        }

    @Test
    fun `a lightning trade never seeds a wizard address`() =
        runTest {
            val presenter = attachWithTrade(prefill = PREFILLED_ADDRESS, settlementMethod = "LN")

            assertEquals("", presenter.uiState.value.paymentData)
            assertFalse(presenter.uiState.value.wasPrefilled)
        }

    @Test
    fun `no stored address leaves the screen exactly as before`() =
        runTest {
            val presenter = attachWithTrade(prefill = null)

            assertEquals("", presenter.uiState.value.paymentData)
            assertFalse(presenter.uiState.value.wasPrefilled)
        }

    @Test
    fun `a successful send consumes the stored prefill`() =
        runTest {
            val presenter = attachWithTrade(prefill = PREFILLED_ADDRESS)
            coEvery { tradesServiceFacade.buyerSendBitcoinPaymentData(any()) } returns Result.success(Unit)

            presenter.onAction(BuyerState1aUiAction.OnConfirmPrefillChange(true))
            presenter.onAction(BuyerState1aUiAction.OnSend)
            advanceUntilIdle()

            assertFalse(
                payoutAddressPrepRepository.mutableData.value.prefillByTradeId
                    .containsKey("trade-1"),
            )
        }

    private suspend fun TestScope.attachWithTrade(
        prefill: String?,
        settlementMethod: String = "MAIN_CHAIN",
    ): BuyerState1aPresenter {
        if (prefill != null) {
            payoutAddressPrepRepository.setPrefill("trade-1", prefill)
        }
        val trade = mockk<TradeItemPresentationModel>(relaxed = true)
        every { trade.tradeId } returns "trade-1"
        every { trade.bitcoinSettlementMethod } returns settlementMethod
        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(trade)

        val presenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade, payoutAddressPrepRepository)
        presenter.onViewAttached()
        advanceUntilIdle()
        return presenter
    }
}
