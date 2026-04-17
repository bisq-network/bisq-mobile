package network.bisq.mobile.presentation.design.trade_export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_completed
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.BtcSatsStyle
import network.bisq.mobile.presentation.common.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.common.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.trade.trade_detail.TradeDetailsHeaderUiAction
import network.bisq.mobile.presentation.trade.trade_detail.TradeDetailsHeaderContent
import network.bisq.mobile.presentation.trade.trade_detail.TradeDetailsHeaderSessionUiState
import network.bisq.mobile.presentation.trade.trade_detail.TradeDetailsHeaderTradeUiState

/**
 * Design POC: Completed trade summary with export functionality.
 *
 * Ports the Bisq2 desktop TradeCompletedTable into a mobile-optimized vertical layout.
 *
 * Desktop has a 5-column header grid + 2-column body grid. On mobile this becomes:
 * - Completion header (icon + title)
 * - Trade summary card with key-value rows using InfoBox/InfoRow
 * - Copyable fields (trade ID, tx ID, block explorer URL) with inline copy icons
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

@Composable
private fun TradeCompletedScreen(
    trade: SimulatedCompletedTrade,
    onExport: () -> Unit,
    onCloseTrade: () -> Unit,
    initialShowDetails: Boolean = true,
) {
    var showDetails by remember(initialShowDetails) { mutableStateOf(initialShowDetails) }
    val (tradeUiState, sessionUiState) = trade.toCompletedHeaderStates(showDetails = showDetails)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        TradeDetailsHeaderContent(
            tradeUiState = tradeUiState,
            sessionUiState = sessionUiState,
            userProfileIconProvider = previewUserProfileIconProvider,
            onAction = { action ->
                when (action) {
                    TradeDetailsHeaderUiAction.ToggleHeader -> showDetails = !showDetails
                    else -> Unit
                }
            },
        )

        BisqGap.V1()

        State4SummarySection(trade = trade)

        BisqGap.V1()

        ActionButtons(onExport = onExport, onCloseTrade = onCloseTrade)
    }
}

@Composable
private fun State4SummarySection(trade: SimulatedCompletedTrade) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_completed,
                isLoading = true,
            )
            BisqText.H5Light("bisqEasy.tradeCompleted.title".i18n())
        }

        BtcSatsText(
            trade.baseAmount,
            label =
                if (trade.isBuyer) {
                    "bisqEasy.tradeCompleted.header.myDirection.buyer".i18n()
                } else {
                    "bisqEasy.tradeCompleted.header.myDirection.seller".i18n()
                },
            style = BtcSatsStyle.TextField,
        )

        BisqTextFieldV0(
            label =
                if (trade.isBuyer) {
                    "bisqEasy.tradeCompleted.header.myOutcome.buyer".i18n()
                } else {
                    "bisqEasy.tradeCompleted.header.myOutcome.seller".i18n()
                },
            value = "${trade.quoteAmount} ${trade.quoteCurrency}",
            enabled = false,
        )
    }
}

private fun SimulatedCompletedTrade.toCompletedHeaderStates(showDetails: Boolean): Pair<TradeDetailsHeaderTradeUiState, TradeDetailsHeaderSessionUiState> {
    val isSell = !isBuyer
    val (leftAmount, leftCode, rightAmount, rightCode) =
        if (isSell) {
            arrayOf(baseAmount, "BTC", quoteAmount, quoteCurrency)
        } else {
            arrayOf(quoteAmount, quoteCurrency, baseAmount, "BTC")
        }

    val formattedDate =
        if (tradeDate.contains(" ")) {
            tradeDate.substringBeforeLast(" ")
        } else {
            tradeDate
        }
    val formattedTime =
        if (tradeDate.contains(" ")) {
            tradeDate.substringAfterLast(" ")
        } else {
            ""
        }

    return TradeDetailsHeaderTradeUiState(
        directionalTitle = if (isBuyer) "Buying from:" else "Selling to:",
        peersUserProfile = createMockUserProfile(peerName),
        peersReputationScore = ReputationScoreVO(totalScore = 980, fiveSystemScore = 4.2, ranking = 34),
        priceDisplay = "$price $priceCurrency",
        formattedDate = formattedDate,
        formattedTime = formattedTime,
        fiatPaymentMethodDisplayString = fiatPaymentMethod,
        bitcoinSettlementMethodDisplayString = bitcoinSettlementMethod,
        shortTradeId = shortTradeId,
        tradeId = tradeId,
        mediatorUserName = null,
        isSell = isSell,
        isSmallScreen = false,
        leftAmountDescription = if (isBuyer) "Pay" else "Send",
        rightAmountDescription = "Receive",
        leftAmount = leftAmount,
        leftCode = leftCode,
        rightAmount = rightAmount,
        rightCode = rightCode,
        isMainChainPayment = bitcoinSettlementMethod == "On-chain",
        peerNetworkAddress = peerNetworkAddress,
    ) to TradeDetailsHeaderSessionUiState(
        showDetails = showDetails,
        isInteractive = true,
        interruptTradeButtonText = "",
        openMediationButtonText = "",
        isInMediation = false,
        paymentProof = txId,
        receiverAddress = bitcoinAddress,
        isCompleted = true,
    )
}

private val previewUserProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

@Composable
private fun ActionButtons(
    onExport: () -> Unit,
    onCloseTrade: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqButton(
            modifier = Modifier.weight(1f),
            text = "Export trade data",
            type = BisqButtonType.Grey,
            onClick = onExport,
        )
        BisqButton(
            modifier = Modifier.weight(1f),
            text = "Close trade",
            onClick = onCloseTrade,
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
        myRole = "Maker",
        peerRole = "Taker",
        tradeDate = "Mar 27, 2026 14:32",
        tradeId = "t-abc123def456ghi789",
        shortTradeId = "t-abc123d",
        txId = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6abcd",
        bitcoinAddress = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
        peerNetworkAddress = "runbtcx3wfygbq2wdde6qzjnpyrqn3gvbks7t5jdymmunxttdvvttpyd.onion",
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
        bitcoinSettlementMethod = "On-Chain",
        myRole = "Taker",
        peerRole = "Maker",
        tradeDate = "Mar 26, 2026 09:15",
        tradeId = "t-xyz789abc123def456",
        shortTradeId = "t-xyz789a",
        txId = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6abcd",
        bitcoinAddress = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
        peerNetworkAddress = "mempool4t6mypeemozyterviq3i5de4kpoua65r3qkn5i3kknu5l2cad.onion",
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
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun BuyerCompleted_CollapsedHeader_Preview() {
    BisqTheme.Preview {
        TradeCompletedScreen(
            trade = sampleBuyerTrade,
            onExport = {},
            onCloseTrade = {},
            initialShowDetails = false,
        )
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
