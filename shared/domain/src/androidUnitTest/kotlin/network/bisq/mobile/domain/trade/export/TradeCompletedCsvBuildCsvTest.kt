package network.bisq.mobile.domain.trade.export

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.domain.formatters.NumberFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class TradeCompletedCsvBuildCsvTest {
    private val simpleHeaders =
        TradeExportCsvHeaders(
            tradeId = "H_trade",
            amountBtc = "H_btc",
            amountInQuote = "H_quote",
            txIdOrPreimage = "H_tx",
            receiverAddressOrInvoice = "H_recv",
            paymentMethod = "H_pm",
        )

    private val proofTestHeaders =
        TradeExportCsvHeaders(
            tradeId = "a",
            amountBtc = "b",
            amountInQuote = "c",
            txIdOrPreimage = "d",
            receiverAddressOrInvoice = "e",
            paymentMethod = "f",
        )

    @Test
    fun buildCsv_twoLines_joinsHeadersAndData_withSingleNewlineBetween() {
        val trade =
            mockTrade(
                tradeId = "T-1",
                baseAmount = 100_000_000L,
                quoteAmount = 10_000L,
                quoteCurrencyCode = "USD",
                paymentMethodCsv = "ONCHAIN / SEPA",
                paymentProof = null,
                bitcoinPaymentData = null,
            )

        val result = TradeCompletedCsv.buildCsv(trade, simpleHeaders)

        val btcCell = "${NumberFormatter.btcFormatForCsvExport(100_000_000L)} BTC"
        val quoteCell = "${NumberFormatter.formatForCsvExport(10_000.0 / 10000.0)} USD"
        val expectedHeader =
            listOf(
                simpleHeaders.tradeId,
                simpleHeaders.amountBtc,
                simpleHeaders.amountInQuote,
                simpleHeaders.txIdOrPreimage,
                simpleHeaders.receiverAddressOrInvoice,
                simpleHeaders.paymentMethod,
            ).joinToString(",") { TradeCompletedCsv.escapeCsvField(it) }
        val expectedData =
            listOf(
                "T-1",
                btcCell,
                quoteCell,
                "N/A",
                "",
                "ONCHAIN / SEPA",
            ).joinToString(",") { TradeCompletedCsv.escapeCsvField(it) }

        assertEquals("$expectedHeader\n$expectedData", result)
    }

    @Test
    fun buildCsv_paymentProof_null_or_blank_becomes_NA() {
        fun assertProof(
            paymentProof: String?,
            expectedProofCell: String,
        ) {
            val trade =
                mockTrade(
                    tradeId = "t",
                    baseAmount = 100_000_000L,
                    quoteAmount = 10_000L,
                    quoteCurrencyCode = "USD",
                    paymentMethodCsv = "m",
                    paymentProof = paymentProof,
                    bitcoinPaymentData = null,
                )
            val btcCell = "${NumberFormatter.btcFormatForCsvExport(100_000_000L)} BTC"
            val quoteCell = "${NumberFormatter.formatForCsvExport(10_000.0 / 10000.0)} USD"
            val expectedData =
                listOf(
                    "t",
                    btcCell,
                    quoteCell,
                    expectedProofCell,
                    "",
                    "m",
                ).joinToString(",") { TradeCompletedCsv.escapeCsvField(it) }
            val expectedHeader =
                listOf(
                    proofTestHeaders.tradeId,
                    proofTestHeaders.amountBtc,
                    proofTestHeaders.amountInQuote,
                    proofTestHeaders.txIdOrPreimage,
                    proofTestHeaders.receiverAddressOrInvoice,
                    proofTestHeaders.paymentMethod,
                ).joinToString(",") { TradeCompletedCsv.escapeCsvField(it) }
            assertEquals(
                "$expectedHeader\n$expectedData",
                TradeCompletedCsv.buildCsv(trade, proofTestHeaders),
            )
        }

        assertProof(null, "N/A")
        assertProof("", "N/A")
        assertProof("   ", "N/A")
        assertProof("  abc  ", "abc")
        assertProof("tx-1", "tx-1")
    }

    @Test
    fun buildCsv_bitcoinPaymentData_emptyWhenNull() {
        val trade =
            mockTrade(
                tradeId = "x",
                baseAmount = 0L,
                quoteAmount = 0L,
                quoteCurrencyCode = "EUR",
                paymentMethodCsv = "m",
                paymentProof = "p",
                bitcoinPaymentData = null,
            )
        val btcCell = "${NumberFormatter.btcFormatForCsvExport(0L)} BTC"
        val quoteCell = "${NumberFormatter.formatForCsvExport(0.0)} EUR"
        val expectedData =
            listOf(
                "x",
                btcCell,
                quoteCell,
                "p",
                "",
                "m",
            ).joinToString(",") { TradeCompletedCsv.escapeCsvField(it) }
        val expectedHeader =
            listOf(
                proofTestHeaders.tradeId,
                proofTestHeaders.amountBtc,
                proofTestHeaders.amountInQuote,
                proofTestHeaders.txIdOrPreimage,
                proofTestHeaders.receiverAddressOrInvoice,
                proofTestHeaders.paymentMethod,
            ).joinToString(",") { TradeCompletedCsv.escapeCsvField(it) }
        assertEquals(
            "$expectedHeader\n$expectedData",
            TradeCompletedCsv.buildCsv(trade, proofTestHeaders),
        )
    }

    @Test
    fun buildCsv_escapesTradeId_whenContainsComma() {
        val headers =
            TradeExportCsvHeaders(
                tradeId = "H1",
                amountBtc = "H2",
                amountInQuote = "H3",
                txIdOrPreimage = "H4",
                receiverAddressOrInvoice = "H5",
                paymentMethod = "H6",
            )
        val trade =
            mockTrade(
                tradeId = "id,with,comma",
                baseAmount = 0L,
                quoteAmount = 0L,
                quoteCurrencyCode = "USD",
                paymentMethodCsv = "pm",
                paymentProof = "proof",
                bitcoinPaymentData = "addr",
            )
        val result = TradeCompletedCsv.buildCsv(trade, headers)
        val dataLine = result.substringAfter('\n')
        assertEquals(
            listOf(
                "id,with,comma",
                "${NumberFormatter.btcFormatForCsvExport(0L)} BTC",
                "${NumberFormatter.formatForCsvExport(0.0)} USD",
                "proof",
                "addr",
                "pm",
            ).joinToString(",") { TradeCompletedCsv.escapeCsvField(it) },
            dataLine,
        )
    }

    @Test
    fun buildCsv_escapesHeader_whenContainsComma() {
        val headers =
            TradeExportCsvHeaders(
                tradeId = "Trade,Id",
                amountBtc = "B",
                amountInQuote = "Q",
                txIdOrPreimage = "T",
                receiverAddressOrInvoice = "R",
                paymentMethod = "P",
            )
        val trade =
            mockTrade(
                tradeId = "t1",
                baseAmount = 1L,
                quoteAmount = 1L,
                quoteCurrencyCode = "USD",
                paymentMethodCsv = "m",
                paymentProof = null,
                bitcoinPaymentData = null,
            )
        val headerLine = TradeCompletedCsv.buildCsv(trade, headers).substringBefore('\n')
        assertEquals(
            listOf(
                "Trade,Id",
                "B",
                "Q",
                "T",
                "R",
                "P",
            ).joinToString(",") { TradeCompletedCsv.escapeCsvField(it) },
            headerLine,
        )
    }

    private fun mockTrade(
        tradeId: String,
        baseAmount: Long,
        quoteAmount: Long,
        quoteCurrencyCode: String,
        paymentMethodCsv: String,
        paymentProof: String?,
        bitcoinPaymentData: String?,
    ): TradeItemPresentationModel {
        val trade = mockk<TradeItemPresentationModel>(relaxed = true)
        every { trade.tradeId } returns tradeId
        every { trade.baseAmount } returns baseAmount
        every { trade.quoteAmount } returns quoteAmount
        every { trade.quoteCurrencyCode } returns quoteCurrencyCode
        every { trade.paymentMethodCsvDisplayString } returns paymentMethodCsv

        val tm = mockk<BisqEasyTradeModel>(relaxed = true)
        every { trade.bisqEasyTradeModel } returns tm
        every { tm.paymentProof } returns MutableStateFlow(paymentProof)
        every { tm.bitcoinPaymentData } returns MutableStateFlow(bitcoinPaymentData)
        return trade
    }
}
