package network.bisq.mobile.domain.trade.export

import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.i18n.i18n

/**
 * Resolved CSV header labels (from i18n) for one trade export row.
 */
data class TradeExportCsvHeaders(
    val tradeId: String,
    val amountBtc: String,
    val amountInQuote: String,
    val txIdOrPreimage: String,
    val receiverAddressOrInvoice: String,
    val paymentMethod: String,
) {
    companion object {
        fun resolveForTrade(trade: TradeItemPresentationModel): TradeExportCsvHeaders =
            TradeExportCsvHeaders(
                tradeId = "bisqEasy.openTrades.table.tradeId".i18n(),
                amountBtc = "bisqEasy.openTrades.table.baseAmount".i18n(),
                amountInQuote = "bisqEasy.openTrades.csv.quoteAmount".i18n(trade.quoteCurrencyCode),
                txIdOrPreimage = "bisqEasy.openTrades.csv.txIdOrPreimage".i18n(),
                receiverAddressOrInvoice = "bisqEasy.openTrades.csv.receiverAddressOrInvoice".i18n(),
                paymentMethod = "bisqEasy.openTrades.csv.paymentMethod".i18n(),
            )
    }
}
