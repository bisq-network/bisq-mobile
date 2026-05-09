package network.bisq.mobile.presentation.tabs.my_trades.closed.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.common.network.TransportTypeEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.TradeDurationFormatter
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcome
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@Composable
fun TradeDetailsDialog(
    item: ClosedTradeListItem,
    onDismiss: () -> Unit,
) {
    BisqDialog(
        onDismissRequest = onDismiss,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BisqText.H5Medium(
                text = "mobile.tradeHistory.details.headline".i18n(),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "action.close".i18n(),
                    tint = BisqTheme.colors.light_grey10,
                )
            }
        }

        BisqGap.V1()

        SectionHeader("mobile.tradeHistory.details.overview".i18n())
        BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)

        DetailRow(
            label = "mobile.tradeHistory.details.tradersAndRole".i18n(),
            value = formatTradersAndRole(item),
            copyValue = item.peersUserProfile.userName,
        )
        StyledDetailRow(
            label = "mobile.tradeHistory.details.amountAndPrice".i18n(),
            value = buildAmountAndPrice(item),
        )
        DetailRow(
            label = "mobile.tradeHistory.details.paymentAndSettlementMethods".i18n(),
            value = formatPaymentMethods(item),
        )
        DetailRow(
            label = "mobile.tradeHistory.details.paymentAccountData".i18n(),
            value = item.paymentAccountData ?: dataNotYetProvided(),
            copyValue = item.paymentAccountData,
            valueDimmed = item.paymentAccountData == null,
        )
        DetailRow(
            label = btcPaymentAddressLabel(item),
            value = item.bitcoinPaymentData ?: dataNotYetProvided(),
            copyValue = item.bitcoinPaymentData,
            valueDimmed = item.bitcoinPaymentData == null,
        )
        if (item.isOnChainSettlement || item.paymentProof != null) {
            DetailRow(
                label = paymentProofLabel(item),
                value = item.paymentProof ?: dataNotYetProvided(),
                copyValue = item.paymentProof,
                valueDimmed = item.paymentProof == null,
            )
        }
        DetailRow(
            label = "mobile.tradeHistory.details.outcome".i18n(),
            value = outcomeLabel(item.outcome),
        )

        BisqGap.V1()

        SectionHeader("mobile.tradeHistory.details.details".i18n())
        BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)

        DetailRow(
            label = "mobile.tradeHistory.details.tradeId".i18n(),
            value = item.tradeId,
            copyValue = item.tradeId,
        )
        DetailRow(
            label = "mobile.tradeHistory.details.tradeDate".i18n(),
            value = item.formattedDateTime,
        )
        formatTradeDuration(item)?.let { duration ->
            DetailRow(
                label = "mobile.tradeHistory.details.tradeDuration".i18n(),
                value = duration,
            )
        }
        DetailRow(
            label = "mobile.tradeHistory.details.offerTypeAndMarket".i18n(),
            value = formatOfferTypeAndMarket(item),
        )
        formatPeerNetworkAddress(item.peersUserProfile)?.let { (display, rawAddress) ->
            DetailRow(
                label = "mobile.tradeHistory.details.peerNetworkAddress".i18n(),
                value = display,
                copyValue = rawAddress,
            )
        }
        item.mediatorUserProfile?.let { mediator ->
            DetailRow(
                label = "mobile.tradeHistory.details.assignedMediator".i18n(),
                value = mediator.userName,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    BisqText.SmallRegularGrey(text = text)
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    copyValue: String? = null,
    valueDimmed: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = BisqUIConstants.ScreenPaddingQuarter),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BisqText.SmallRegularGrey(text = label)
            if (valueDimmed) {
                BisqText.BaseRegularGrey(text = value)
            } else {
                BisqText.BaseRegular(text = value)
            }
        }
        if (!copyValue.isNullOrBlank()) {
            CopyIconButton(value = copyValue)
        }
    }
}

private fun formatTradersAndRole(item: ClosedTradeListItem): String {
    val mePrefix = "mobile.tradeHistory.details.me".i18n()
    val peerPrefix = "mobile.tradeHistory.details.peer".i18n()
    val peer = item.peersUserProfile.userName
    val me =
        if (item.myUserName.isNotBlank()) {
            "${item.myUserName} (${item.myRole.lowercase()})"
        } else {
            item.myRole
        }
    return "$mePrefix $me / $peerPrefix $peer"
}

@Composable
private fun StyledDetailRow(
    label: String,
    value: AnnotatedString,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = BisqUIConstants.ScreenPaddingQuarter),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BisqText.SmallRegularGrey(text = label)
            BisqText.StyledText(
                text = value,
                style = BisqTheme.typography.baseRegular,
            )
        }
    }
}

@Composable
private fun buildAmountAndPrice(item: ClosedTradeListItem): AnnotatedString {
    val white = BisqTheme.colors.white
    val grey = BisqTheme.colors.mid_grey20
    val smallSize = BisqTheme.typography.smallRegular.fontSize
    val baseSize = BisqTheme.typography.baseRegular.fontSize

    val whiteSmall = SpanStyle(color = white, fontSize = smallSize)
    val greySmall = SpanStyle(color = grey, fontSize = smallSize)
    val greyBase = SpanStyle(color = grey, fontSize = baseSize)

    val quoteAmount = item.formattedQuoteAmount
    val baseAmount = item.formattedBaseAmount
    val priceValue = item.formattedPriceValue
    val priceCodes = item.priceMarketCodes

    return buildAnnotatedString {
        append(quoteAmount)
        if (item.quoteCurrencyCode.isNotBlank()) {
            withStyle(whiteSmall) { append(" ${item.quoteCurrencyCode}") }
        }
        withStyle(greyBase) { append(" (") }
        withStyle(greyBase) { append(baseAmount) }
        withStyle(greySmall) { append(" BTC") }
        withStyle(greyBase) { append(")") }
        if (priceValue.isNotBlank()) {
            withStyle(greyBase) { append(" @ ") }
            append(priceValue)
            if (priceCodes.isNotBlank()) {
                withStyle(whiteSmall) { append(" $priceCodes") }
            }
        }
    }
}

private fun formatPaymentMethods(item: ClosedTradeListItem): String = "${item.fiatPaymentMethodDisplay} / ${item.bitcoinSettlementMethodDisplay}"

private fun formatOfferTypeAndMarket(item: ClosedTradeListItem): String {
    // Offer direction = maker's direction. If I'm maker, my buyer/seller side is the offer side.
    // If I'm taker, the offer side is the opposite of my side.
    val isBuyOffer = item.isMaker == item.isBuyer
    val offerType =
        if (isBuyOffer) {
            "mobile.tradeHistory.details.offerType.buy".i18n()
        } else {
            "mobile.tradeHistory.details.offerType.sell".i18n()
        }
    val ccy = item.quoteCurrencyCode.ifBlank { "" }
    val market =
        if (ccy.isNotBlank()) {
            "mobile.tradeHistory.details.market".i18n(ccy)
        } else {
            ""
        }
    return if (market.isNotBlank()) "$offerType / $market" else offerType
}

private fun formatTradeDuration(item: ClosedTradeListItem): String? {
    val completed = item.tradeCompletedDate ?: return null
    return TradeDurationFormatter.formatAge(completed, item.takeOfferDate).takeIf { it.isNotBlank() }
}

private fun btcPaymentAddressLabel(item: ClosedTradeListItem): String =
    if (item.isOnChainSettlement) {
        "mobile.tradeHistory.details.btcPaymentAddress".i18n()
    } else {
        "mobile.tradeHistory.details.lightningInvoice".i18n()
    }

private fun paymentProofLabel(item: ClosedTradeListItem): String =
    if (item.isOnChainSettlement) {
        "mobile.tradeHistory.details.txId".i18n()
    } else {
        "mobile.tradeHistory.details.lightningPreImage".i18n()
    }

private fun dataNotYetProvided(): String = "mobile.tradeHistory.details.dataNotYetProvided".i18n()

private fun outcomeLabel(outcome: TradeOutcome): String =
    when (outcome) {
        TradeOutcome.COMPLETED -> "mobile.tradeHistory.outcome.completed".i18n()
        TradeOutcome.CANCELLED -> "mobile.tradeHistory.outcome.cancelled".i18n()
        TradeOutcome.REJECTED -> "mobile.tradeHistory.outcome.rejected".i18n()
        TradeOutcome.FAILED -> "mobile.tradeHistory.outcome.failed".i18n()
    }

private data class PeerNetworkAddressDisplay(
    val display: String,
    val rawAddress: String,
)

private fun formatPeerNetworkAddress(profile: UserProfileVO): PeerNetworkAddressDisplay? {
    // Privacy-aware deterministic transport pick: prefer onion / I2P over clearnet so the
    // displayed address never leaks a clearnet endpoint when an anonymous one is available.
    val map = profile.networkId.addressByTransportTypeMap.map
    val priority = listOf(TransportTypeEnum.TOR, TransportTypeEnum.I2P, TransportTypeEnum.CLEAR)
    val transport = priority.firstOrNull { it in map } ?: return null
    val addressVo = map.getValue(transport)
    val address = "${addressVo.host}:${addressVo.port}"
    val key =
        when (transport) {
            TransportTypeEnum.CLEAR -> "mobile.tradeHistory.details.networkAddress.clear"
            TransportTypeEnum.TOR -> "mobile.tradeHistory.details.networkAddress.tor"
            TransportTypeEnum.I2P -> "mobile.tradeHistory.details.networkAddress.i2p"
        }
    return PeerNetworkAddressDisplay(
        display = key.i18n(address),
        rawAddress = address,
    )
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeDetailsDialog_Preview() {
    BisqTheme.Preview {
        BisqScaffold {
            TradeDetailsDialog(
                item =
                    ClosedTradeListItem(
                        tradeId = "1514b30a-d637-3609-bad0-4fa3dcc1a8aa",
                        peersUserProfile = createMockUserProfile("Alice"),
                        peersReputationScore = ReputationScoreVO(1000L, 4.5, 12),
                        mediatorUserProfile = null,
                        myUserProfile = createMockUserProfile("Bob"),
                        priceQuote = null,
                        baseAmount = 7582L,
                        fiatPaymentMethod = "F2F",
                        bitcoinSettlementMethod = "MAIN_CHAIN",
                        isMaker = true,
                        isBuyer = true,
                        outcome = TradeOutcome.COMPLETED,
                        takeOfferDate = 1745740000000L,
                        tradeCompletedDate = 1745740078000L,
                        quoteAmount = 8972_00L,
                        paymentAccountData = "some synthetic data",
                        bitcoinPaymentData = "anobviouslywrongadress",
                        paymentProof = "anobviouslywrongtansactionId",
                    ),
                onDismiss = {},
            )
        }
    }
}
