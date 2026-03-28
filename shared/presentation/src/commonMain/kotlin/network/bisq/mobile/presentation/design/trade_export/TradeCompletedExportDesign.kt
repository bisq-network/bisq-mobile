package network.bisq.mobile.presentation.design.trade_export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_completed
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Design POC: Completed trade summary with export functionality.
 *
 * Ports the Bisq2 desktop TradeCompletedTable into a mobile-optimized vertical layout.
 *
 * Desktop has a 5-column header grid + 2-column body grid. On mobile this becomes:
 * - Completion header (icon + title)
 * - Trade summary card with key-value rows using InfoBox/InfoRow
 * - Copyable fields (trade ID, tx ID) with inline copy icons
 * - Two action buttons: "Export trade data" (share sheet) + "Close trade"
 *
 * ## Export flow (no additional UI needed)
 *
 * 1. User taps "Export trade data"
 * 2. App generates CSV with same 6 fields as desktop (see OpenTradesUtils.java):
 *    Trade ID, Base Amount, Quote Amount, Tx ID/Preimage, Receiver Address/Invoice,
 *    Payment Method
 * 3. App writes a temp file: `BisqEasy-trade-{shortTradeId}.csv`
 * 4. OS share sheet opens via platform API:
 *    - Android: `Intent.ACTION_SEND` with `text/csv` MIME type
 *    - iOS: `UIActivityViewController` with the file URL
 * 5. User picks destination (Files, email, clipboard, AirDrop, etc.)
 *
 * No intermediate preview or picker screen is needed — the OS share sheet
 * natively provides copy, save, and send options on both platforms.
 *
 * ## Implementation notes
 *
 * - Presenter: add `onExportTrade()` to `State4Presenter`, generate CSV string
 *   using the same field order as `OpenTradesUtils.exportTrade()` in bisq2 desktop
 * - Platform expect/actual: `ShareService` interface with `shareFile(path, mimeType)`
 *   implemented per platform (Android Intent / iOS UIActivityViewController)
 * - The existing `TradesServiceFacade.exportTradeDate()` stub should be replaced
 *   with local CSV generation + share (no backend call needed)
 */

private data class SimulatedCompletedTrade(
    val isBuyer: Boolean,
    val peerName: String,
    val baseAmount: String,
    val quoteAmount: String,
    val quoteCurrency: String,
    val price: String,
    val priceCurrency: String,
    val fiatPaymentMethod: String,
    val bitcoinSettlementMethod: String,
    val tradeDate: String,
    val tradeDuration: String,
    val tradeId: String,
    val shortTradeId: String,
    val txId: String?,
    val bitcoinAddress: String?,
)

@Composable
private fun TradeCompletedScreen(
    trade: SimulatedCompletedTrade,
    onExport: () -> Unit,
    onCloseTrade: () -> Unit,
    onCopyValue: (String) -> Unit,
    onOpenExplorer: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        CompletionHeader(trade)

        BisqGap.V2()

        TradeSummaryCard(
            trade = trade,
            onCopyValue = onCopyValue,
            onOpenExplorer = onOpenExplorer,
        )

        BisqGap.V2()

        ActionButtons(onExport = onExport, onCloseTrade = onCloseTrade)
    }
}

@Composable
private fun CompletionHeader(trade: SimulatedCompletedTrade) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularLoadingImage(
            image = Res.drawable.trade_completed,
            isLoading = true,
        )
        Column {
            BisqText.H5Light("Trade completed")
            BisqGap.VQuarter()
            BisqText.SmallLight(
                text = "Traded with ${trade.peerName}",
                color = BisqTheme.colors.mid_grey20,
            )
        }
    }
}

@Composable
private fun TradeSummaryCard(
    trade: SimulatedCompletedTrade,
    onCopyValue: (String) -> Unit,
    onOpenExplorer: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val directionLabel = if (trade.isBuyer) "I bought" else "I sold"
        val outcomeLabel = if (trade.isBuyer) "I paid" else "I received"

        InfoRow(
            label1 = directionLabel,
            value1 = "${trade.baseAmount} BTC",
            label2 = outcomeLabel,
            value2 = "${trade.quoteAmount} ${trade.quoteCurrency}",
        )

        InfoRow(
            label1 = "Trade price",
            value1 = "${trade.price} ${trade.priceCurrency}",
            label2 = "Payment method",
            value2 = "${trade.fiatPaymentMethod} / ${trade.bitcoinSettlementMethod}",
        )

        BisqHDivider()

        InfoRow(
            label1 = "Take offer date",
            value1 = trade.tradeDate,
            label2 = "Trade duration",
            value2 = trade.tradeDuration,
        )

        CopyableInfoRow(
            label = "Trade ID",
            value = trade.shortTradeId,
            fullValue = trade.tradeId,
            onCopy = onCopyValue,
        )

        if (!trade.txId.isNullOrBlank()) {
            val isOnChainTx = trade.txId.matches(Regex("^[0-9a-fA-F]{64}$"))
            CopyableInfoRow(
                label = if (isOnChainTx) "Transaction ID" else "Payment proof",
                value = trade.txId.take(12) + "\u2026",
                fullValue = trade.txId,
                onCopy = onCopyValue,
            )

            if (isOnChainTx) {
                BisqText.SmallLight(
                    text = "View in block explorer \u2192",
                    color = BisqTheme.colors.primary,
                    modifier = Modifier.clickable { onOpenExplorer(trade.txId) },
                )
            }
        }

        if (!trade.bitcoinAddress.isNullOrBlank()) {
            CopyableInfoRow(
                label = "Receiver address",
                value = trade.bitcoinAddress.take(16) + "\u2026",
                fullValue = trade.bitcoinAddress,
                onCopy = onCopyValue,
            )
        }
    }
}

@Composable
private fun CopyableInfoRow(
    label: String,
    value: String,
    fullValue: String,
    onCopy: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            InfoBox(label = label, value = value)
        }
        IconButton(
            onClick = { onCopy(fullValue) },
            modifier =
                Modifier
                    .size(BisqUIConstants.ScreenPadding2X)
                    .semantics { contentDescription = "Copy" },
        ) {
            CopyIcon()
        }
    }
}

@Composable
private fun ActionButtons(
    onExport: () -> Unit,
    onCloseTrade: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        BisqButton(
            text = "Export trade data",
            type = BisqButtonType.Grey,
            onClick = onExport,
            fullWidth = true,
        )
        BisqButton(
            text = "Close trade",
            onClick = onCloseTrade,
            fullWidth = true,
        )
    }
}

private val sampleBuyerTrade =
    SimulatedCompletedTrade(
        isBuyer = true,
        peerName = "SatoshiFan42",
        baseAmount = "0.00500000",
        quoteAmount = "342.10",
        quoteCurrency = "USD",
        price = "68,420.00",
        priceCurrency = "USD/BTC",
        fiatPaymentMethod = "SEPA",
        bitcoinSettlementMethod = "On-chain",
        tradeDate = "Mar 27, 2026 14:32",
        tradeDuration = "2h 15m",
        tradeId = "t-abc123def456ghi789",
        shortTradeId = "t-abc123d",
        txId = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6abcd",
        bitcoinAddress = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
    )

private val sampleSellerTrade =
    SimulatedCompletedTrade(
        isBuyer = false,
        peerName = "LightningLover99",
        baseAmount = "0.01200000",
        quoteAmount = "820.00",
        quoteCurrency = "EUR",
        price = "68,333.00",
        priceCurrency = "EUR/BTC",
        fiatPaymentMethod = "Revolut",
        bitcoinSettlementMethod = "Lightning",
        tradeDate = "Mar 26, 2026 09:15",
        tradeDuration = "45m",
        tradeId = "t-xyz789abc123def456",
        shortTradeId = "t-xyz789a",
        txId = "lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqf",
        bitcoinAddress = null,
    )

@ExcludeFromCoverage
@Preview
@Composable
private fun BuyerCompleted_Preview() {
    BisqTheme.Preview {
        TradeCompletedScreen(
            trade = sampleBuyerTrade,
            onExport = {},
            onCloseTrade = {},
            onCopyValue = {},
            onOpenExplorer = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun SellerCompleted_Preview() {
    BisqTheme.Preview {
        TradeCompletedScreen(
            trade = sampleSellerTrade,
            onExport = {},
            onCloseTrade = {},
            onCopyValue = {},
            onOpenExplorer = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun SummaryCard_Buyer_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Buyer trade summary (mainchain with tx ID):",
                color = BisqTheme.colors.light_grey10,
            )
            BisqGap.VHalf()
            TradeSummaryCard(
                trade = sampleBuyerTrade,
                onCopyValue = {},
                onOpenExplorer = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun SummaryCard_Seller_Lightning_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Seller trade summary (Lightning, no tx ID):",
                color = BisqTheme.colors.light_grey10,
            )
            BisqGap.VHalf()
            TradeSummaryCard(
                trade = sampleSellerTrade,
                onCopyValue = {},
                onOpenExplorer = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun ActionButtons_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            ActionButtons(onExport = {}, onCloseTrade = {})
        }
    }
}
