package network.bisq.mobile.data.replicated.trade.bisq_easy

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.replicated.contract.RoleEnum
import network.bisq.mobile.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BisqEasyTradeModelTest {
    private fun model(
        tradeState: BisqEasyTradeStateEnum = BisqEasyTradeStateEnum.INIT,
        interruptTradeInitiator: RoleEnum? = null,
        paymentAccountData: String? = null,
        bitcoinPaymentData: String? = null,
        paymentProof: String? = null,
        errorMessage: String? = null,
        errorStackTrace: String? = null,
        peersErrorMessage: String? = null,
        peersErrorStackTrace: String? = null,
        tradeCompletedDate: Long? = null,
    ): BisqEasyTradeModel {
        val dto = mockk<BisqEasyTradeDto>(relaxed = true)
        every { dto.tradeRole } returns TradeRoleEnum.BUYER_AS_TAKER
        every { dto.tradeState } returns tradeState
        every { dto.interruptTradeInitiator } returns interruptTradeInitiator
        every { dto.paymentAccountData } returns paymentAccountData
        every { dto.bitcoinPaymentData } returns bitcoinPaymentData
        every { dto.paymentProof } returns paymentProof
        every { dto.errorMessage } returns errorMessage
        every { dto.errorStackTrace } returns errorStackTrace
        every { dto.peersErrorMessage } returns peersErrorMessage
        every { dto.peersErrorStackTrace } returns peersErrorStackTrace
        every { dto.tradeCompletedDate } returns tradeCompletedDate
        return BisqEasyTradeModel(dto)
    }

    @Test
    fun `mutable trade state is not exposed as MutableStateFlow`() {
        val model = model()
        assertFalse(model.tradeState is MutableStateFlow<*>, "tradeState must be read-only")
        assertFalse(model.interruptTradeInitiator is MutableStateFlow<*>, "interruptTradeInitiator must be read-only")
        assertFalse(model.paymentAccountData is MutableStateFlow<*>, "paymentAccountData must be read-only")
        assertFalse(model.bitcoinPaymentData is MutableStateFlow<*>, "bitcoinPaymentData must be read-only")
        assertFalse(model.paymentProof is MutableStateFlow<*>, "paymentProof must be read-only")
        assertFalse(model.errorMessage is MutableStateFlow<*>, "errorMessage must be read-only")
        assertFalse(model.errorStackTrace is MutableStateFlow<*>, "errorStackTrace must be read-only")
        assertFalse(model.peersErrorMessage is MutableStateFlow<*>, "peersErrorMessage must be read-only")
        assertFalse(model.peersErrorStackTrace is MutableStateFlow<*>, "peersErrorStackTrace must be read-only")
        assertFalse(model.tradeCompletedDate is MutableStateFlow<*>, "tradeCompletedDate must be read-only")
    }

    @Test
    fun `initial values are seeded from the dto`() {
        val model =
            model(
                tradeState = BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST,
                interruptTradeInitiator = RoleEnum.MAKER,
                paymentAccountData = "account",
                bitcoinPaymentData = "bc1qaddr",
                paymentProof = "txid",
                errorMessage = "err",
                errorStackTrace = "trace",
                peersErrorMessage = "peer-err",
                peersErrorStackTrace = "peer-trace",
                tradeCompletedDate = 42L,
            )

        assertEquals(BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST, model.tradeState.value)
        assertEquals(RoleEnum.MAKER, model.interruptTradeInitiator.value)
        assertEquals("account", model.paymentAccountData.value)
        assertEquals("bc1qaddr", model.bitcoinPaymentData.value)
        assertEquals("txid", model.paymentProof.value)
        assertEquals("err", model.errorMessage.value)
        assertEquals("trace", model.errorStackTrace.value)
        assertEquals("peer-err", model.peersErrorMessage.value)
        assertEquals("peer-trace", model.peersErrorStackTrace.value)
        assertEquals(42L, model.tradeCompletedDate.value)
    }

    @Test
    fun `setters update the exposed state`() {
        val model = model()

        model.setTradeState(BisqEasyTradeStateEnum.BTC_CONFIRMED)
        model.setInterruptTradeInitiator(RoleEnum.TAKER)
        model.setPaymentAccountData("account")
        model.setBitcoinPaymentData("bc1qaddr")
        model.setPaymentProof("txid")
        model.setErrorMessage("err")
        model.setErrorStackTrace("trace")
        model.setPeersErrorMessage("peer-err")
        model.setPeersErrorStackTrace("peer-trace")
        model.setTradeCompletedDate(99L)

        assertEquals(BisqEasyTradeStateEnum.BTC_CONFIRMED, model.tradeState.value)
        assertEquals(RoleEnum.TAKER, model.interruptTradeInitiator.value)
        assertEquals("account", model.paymentAccountData.value)
        assertEquals("bc1qaddr", model.bitcoinPaymentData.value)
        assertEquals("txid", model.paymentProof.value)
        assertEquals("err", model.errorMessage.value)
        assertEquals("trace", model.errorStackTrace.value)
        assertEquals("peer-err", model.peersErrorMessage.value)
        assertEquals("peer-trace", model.peersErrorStackTrace.value)
        assertEquals(99L, model.tradeCompletedDate.value)
    }
}
