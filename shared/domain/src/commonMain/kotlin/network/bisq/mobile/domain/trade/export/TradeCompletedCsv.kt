package network.bisq.mobile.domain.trade.export

import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.formatters.NumberFormatter

/**
 * Builds a UTF-8 CSV matching bisq2 desktop export: one header row + one data row.
 * Headers must be pre-resolved via i18n (see [TradeExportCsvHeaders]).
 */
object TradeCompletedCsv {
    fun buildCsv(
        trade: TradeItemPresentationModel,
        headers: TradeExportCsvHeaders,
    ): String {
        val paymentProof =
            trade.bisqEasyTradeModel.paymentProof.value
                .orEmpty()
                .trim()
                .ifEmpty { "N/A" }
        val bitcoinPaymentData =
            trade.bisqEasyTradeModel.bitcoinPaymentData.value
                .orEmpty()
        val amountBtcCell = "${NumberFormatter.btcFormatForCsvExport(trade.baseAmount)} BTC"
        val quoteAmountFormatted =
            NumberFormatter.formatForCsvExport(trade.quoteAmount.toDouble() / 10000.0)
        val amountQuoteCell = "$quoteAmountFormatted ${trade.quoteCurrencyCode}"
        val paymentMethodCell = trade.paymentMethodCsvDisplayString

        val headerLine =
            listOf(
                headers.tradeId,
                headers.amountBtc,
                headers.amountInQuote,
                headers.txIdOrPreimage,
                headers.receiverAddressOrInvoice,
                headers.paymentMethod,
            ).joinToString(",") { escapeCsvField(it) }

        val dataLine =
            listOf(
                trade.tradeId,
                amountBtcCell,
                amountQuoteCell,
                paymentProof,
                bitcoinPaymentData,
                paymentMethodCell,
            ).joinToString(",") { escapeCsvField(it) }

        return "$headerLine\n$dataLine"
    }

    internal fun escapeCsvField(value: String): String {
        if (value.none { it == '"' || it == ',' || it == '\n' || it == '\r' }) {
            return value
        }
        val doubled = value.replace("\"", "\"\"")
        return "\"$doubled\""
    }
}
