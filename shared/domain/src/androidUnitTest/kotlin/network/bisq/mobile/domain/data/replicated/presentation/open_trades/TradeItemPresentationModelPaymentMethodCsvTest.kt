package network.bisq.mobile.data.replicated.presentation.open_trades

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class TradeItemPresentationModelPaymentMethodCsvTest {
    @Test
    fun paymentMethodCsvDisplayString_usesCsvStrings_whenBothPresent() {
        val model =
            tradeItemPresentationModel(
                bitcoinSettlementMethodCsvDisplayString = "BTC_CSV",
                bitcoinSettlementMethodDisplayString = "BTC_DISPLAY",
                fiatPaymentMethodCsvDisplayString = "FIAT_CSV",
                fiatPaymentMethodDisplayString = "FIAT_DISPLAY",
            )

        assertEquals("BTC_CSV / FIAT_CSV", model.paymentMethodCsvDisplayString)
    }

    @Test
    fun paymentMethodCsvDisplayString_fallsBackToBitcoinDisplay_whenBtcCsvEmpty() {
        val model =
            tradeItemPresentationModel(
                bitcoinSettlementMethodCsvDisplayString = "",
                bitcoinSettlementMethodDisplayString = "BTC_DISPLAY",
                fiatPaymentMethodCsvDisplayString = "FIAT_CSV",
                fiatPaymentMethodDisplayString = "FIAT_DISPLAY",
            )

        assertEquals("BTC_DISPLAY / FIAT_CSV", model.paymentMethodCsvDisplayString)
    }

    @Test
    fun paymentMethodCsvDisplayString_fallsBackToFiatDisplay_whenFiatCsvEmpty() {
        val model =
            tradeItemPresentationModel(
                bitcoinSettlementMethodCsvDisplayString = "BTC_CSV",
                bitcoinSettlementMethodDisplayString = "BTC_DISPLAY",
                fiatPaymentMethodCsvDisplayString = "",
                fiatPaymentMethodDisplayString = "FIAT_DISPLAY",
            )

        assertEquals("BTC_CSV / FIAT_DISPLAY", model.paymentMethodCsvDisplayString)
    }

    @Test
    fun paymentMethodCsvDisplayString_fallsBackToBothDisplays_whenBothCsvEmpty() {
        val model =
            tradeItemPresentationModel(
                bitcoinSettlementMethodCsvDisplayString = "",
                bitcoinSettlementMethodDisplayString = "BTC_DISPLAY",
                fiatPaymentMethodCsvDisplayString = "",
                fiatPaymentMethodDisplayString = "FIAT_DISPLAY",
            )

        assertEquals("BTC_DISPLAY / FIAT_DISPLAY", model.paymentMethodCsvDisplayString)
    }

    @Test
    fun paymentMethodCsvDisplayString_fallsBackToDisplays_whenBothCsvNull() {
        val model =
            tradeItemPresentationModel(
                bitcoinSettlementMethodCsvDisplayString = null,
                bitcoinSettlementMethodDisplayString = "BTC_DISPLAY",
                fiatPaymentMethodCsvDisplayString = null,
                fiatPaymentMethodDisplayString = "FIAT_DISPLAY",
            )

        assertEquals("BTC_DISPLAY / FIAT_DISPLAY", model.paymentMethodCsvDisplayString)
    }

    private fun tradeItemPresentationModel(
        bitcoinSettlementMethodCsvDisplayString: String?,
        bitcoinSettlementMethodDisplayString: String,
        fiatPaymentMethodCsvDisplayString: String?,
        fiatPaymentMethodDisplayString: String,
    ): TradeItemPresentationModel {
        val dto = mockk<TradeItemPresentationDto>(relaxed = true)
        every { dto.bitcoinSettlementMethodCsvDisplayString } returns bitcoinSettlementMethodCsvDisplayString
        every { dto.bitcoinSettlementMethodDisplayString } returns bitcoinSettlementMethodDisplayString
        every { dto.fiatPaymentMethodCsvDisplayString } returns fiatPaymentMethodCsvDisplayString
        every { dto.fiatPaymentMethodDisplayString } returns fiatPaymentMethodDisplayString

        return TradeItemPresentationModel(
            tradeItemPresentationDto = dto,
            bisqEasyOpenTradeChannelModel = mockk(relaxed = true),
            bisqEasyTradeModel = mockk(relaxed = true),
        )
    }
}
