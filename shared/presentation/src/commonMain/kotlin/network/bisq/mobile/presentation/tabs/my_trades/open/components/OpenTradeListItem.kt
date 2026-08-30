package network.bisq.mobile.presentation.tabs.my_trades.open.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.contract.BisqEasyContractVO
import network.bisq.mobile.data.replicated.contract.PartyVO
import network.bisq.mobile.data.replicated.contract.RoleEnum
import network.bisq.mobile.data.replicated.identity.IdentityVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeDto
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradePartyVO
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileRow
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
@Composable
fun OpenTradeListItem(
    item: TradeItemPresentationModel,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    unreadCount: Int,
    onSelect: () -> Unit,
    onPeerProfileClick: () -> Unit,
    isOutOfSync: Boolean = false,
) {
    val hasNotifications = unreadCount > 0

    OpenTradeCard(
        borderWidth = 6.dp,
        borderColor = BisqTheme.colors.yellow,
        hasNotifications = hasNotifications,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .debouncedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSelect,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f),
            ) {
                BisqText.BaseLightGrey(
                    text =
                        item.directionalTitle
                            .uppercase()
                            .replace(":", ""),
                    // 'Buying from:' or 'Selling to:'
                )
                Row(modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)) {
                    UserProfileRow(
                        userProfile = item.peersUserProfile,
                        userProfileIconProvider = userProfileIconProvider,
                        reputation = item.peersReputationScore,
                        showUserName = true,
                        badgeCount = unreadCount,
                        onIconClick = onPeerProfileClick,
                    )
                }
                BisqText.SmallLightGrey("${item.formattedDate} ${item.formattedTime}")
                BisqText.SmallLightGrey("mobile.bisqEasy.openTrades.title".i18n(item.shortTradeId))
                if (isOutOfSync) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WarningIcon(modifier = Modifier.size(16.dp))
                        BisqGap.HHalfQuarter()
                        BisqText.SmallMedium(
                            text = "mobile.bisqEasy.openTrades.outOfSync.listTag".i18n(),
                            color = BisqTheme.colors.warning,
                        )
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f),
            ) {
                BisqText.LargeRegular(
                    text = item.quoteAmountWithCode,
                    color = BisqTheme.colors.primary,
                )
                BisqGap.VHalf()
                val priceDisplay =
                    PriceSpecFormatter.formatPriceWithSpec(
                        item.formattedPrice,
                        item.bisqEasyOffer.priceSpec,
                    )
                Row(modifier = Modifier.padding(top = 1.dp)) {
                    if (priceDisplay.length > 18) {
                        BisqText.XSmallRegularGrey("@ ")
                        BisqText.XSmallRegular(priceDisplay)
                    } else {
                        BisqText.SmallRegularGrey("@ ")
                        BisqText.SmallRegular(priceDisplay)
                    }
                }
                BisqGap.VQuarter()
                BtcSatsText(item.formattedBaseAmount)
                BisqGap.VHalf()
                PaymentMethods(
                    listOf(item.bitcoinSettlementMethod),
                    listOf(item.fiatPaymentMethod),
                )
            }
        }
    }
}

private val previewUserProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

/**
 * Builds a real [TradeItemPresentationModel] for the previews below and for `OpenTradeListItemUiTest`,
 * mirroring `createMockOfferItem` in `OfferCard.kt`. The model wraps a [BisqEasyTradeModel], so there
 * is no shorter way to get one — but every type in the chain is a plain data class. `internal` rather
 * than private only so the test can share this one fixture.
 *
 * `channelModel` is null on purpose: this item only ever reads `bisqEasyOffer`, which falls back to
 * the contract's offer. Touching `bisqEasyOpenTradeChannelModel` would throw.
 */
internal fun createMockTradeItem(
    tradeRole: TradeRoleEnum = TradeRoleEnum.BUYER_AS_TAKER,
    peerName: String = "Satoshi",
    quoteCurrencyCode: String = "EUR",
    formattedPrice: String = "50,000",
    formattedQuoteAmount: String = "500",
    reputationScore: Long = 1000L,
): TradeItemPresentationModel {
    val peerProfile = createMockUserProfile(peerName)
    val myProfile = createMockUserProfile("Me")
    val makerProfile = if (tradeRole.isTaker) peerProfile else myProfile
    val takerProfile = if (tradeRole.isTaker) myProfile else peerProfile

    val market = MarketVO("BTC", quoteCurrencyCode, "Bitcoin", quoteCurrencyCode)
    val priceSpec = FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(50_000L, market))
    val bitcoinPaymentMethod = BitcoinPaymentMethodSpecVO("MAIN_CHAIN", null)
    val fiatPaymentMethod = FiatPaymentMethodSpecVO("SEPA", null)

    val offer =
        BisqEasyOfferVO(
            id = "preview-offer-1",
            date = 0L,
            makerNetworkId = makerProfile.networkId,
            // The maker takes the side opposite to mine whenever I am the taker.
            direction = if (tradeRole.isBuyer == tradeRole.isTaker) DirectionEnum.SELL else DirectionEnum.BUY,
            market = market,
            amountSpec = QuoteSideFixedAmountSpecVO(500_00),
            priceSpec = priceSpec,
            protocolTypes = emptyList(),
            baseSidePaymentMethodSpecs = listOf(bitcoinPaymentMethod),
            quoteSidePaymentMethodSpecs = listOf(fiatPaymentMethod),
            offerOptions = emptyList(),
            supportedLanguageCodes = listOf("en"),
        )

    val trade =
        BisqEasyTradeDto(
            contract =
                BisqEasyContractVO(
                    takeOfferDate = 0L,
                    offer = offer,
                    maker = PartyVO(RoleEnum.MAKER, makerProfile.networkId),
                    taker = PartyVO(RoleEnum.TAKER, takerProfile.networkId),
                    baseSideAmount = 1_000_000L,
                    quoteSideAmount = 500_00L,
                    baseSidePaymentMethodSpec = bitcoinPaymentMethod,
                    quoteSidePaymentMethodSpec = fiatPaymentMethod,
                    mediator = null,
                    priceSpec = priceSpec,
                    marketPrice = 50_000L,
                ),
            // At least 8 characters — the model derives `shortId` with `substring(0, 8)`.
            id = "preview-trade-1",
            tradeRole = tradeRole,
            myIdentity = IdentityVO(tag = "preview", networkId = myProfile.networkId),
            taker = BisqEasyTradePartyVO(takerProfile.networkId),
            maker = BisqEasyTradePartyVO(makerProfile.networkId),
            tradeState = BisqEasyTradeStateEnum.INIT,
            paymentAccountData = null,
            bitcoinPaymentData = null,
            paymentProof = null,
            interruptTradeInitiator = null,
            errorMessage = null,
            errorStackTrace = null,
            peersErrorMessage = null,
            peersErrorStackTrace = null,
        )

    return TradeItemPresentationModel(
        channelModel = null,
        bisqEasyTradeModel = BisqEasyTradeModel(trade),
        makerUserProfile = makerProfile,
        takerUserProfile = takerProfile,
        formattedDate = "15 Jan 2024",
        formattedTime = "14:32",
        market = "BTC/$quoteCurrencyCode",
        price = 50_000L,
        formattedPrice = formattedPrice,
        baseAmount = 1_000_000L,
        formattedBaseAmount = "0.01000000",
        quoteAmount = 500_00L,
        formattedQuoteAmount = formattedQuoteAmount,
        bitcoinSettlementMethod = "MAIN_CHAIN",
        bitcoinSettlementMethodDisplayString = "Bitcoin",
        fiatPaymentMethod = "SEPA",
        fiatPaymentMethodDisplayString = "SEPA",
        isFiatPaymentMethodCustom = false,
        formattedMyRole = "",
        peersReputationScore =
            ReputationScoreVO(
                totalScore = reputationScore,
                fiveSystemScore = 5.0,
                ranking = 42,
            ),
    )
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OpenTradeListItem_BuyerPreview() {
    BisqTheme.Preview {
        OpenTradeListItem(
            item = createMockTradeItem(tradeRole = TradeRoleEnum.BUYER_AS_TAKER),
            userProfileIconProvider = previewUserProfileIconProvider,
            unreadCount = 0,
            onSelect = {},
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OpenTradeListItem_OutOfSyncPreview() {
    BisqTheme.Preview {
        OpenTradeListItem(
            item = createMockTradeItem(tradeRole = TradeRoleEnum.BUYER_AS_TAKER),
            userProfileIconProvider = previewUserProfileIconProvider,
            unreadCount = 0,
            onSelect = {},
            onPeerProfileClick = {},
            isOutOfSync = true,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OpenTradeListItem_SellerWithUnreadPreview() {
    BisqTheme.Preview {
        OpenTradeListItem(
            item =
                createMockTradeItem(
                    tradeRole = TradeRoleEnum.SELLER_AS_MAKER,
                    peerName = "BitcoinTrader",
                ),
            userProfileIconProvider = previewUserProfileIconProvider,
            unreadCount = 3,
            onSelect = {},
            onPeerProfileClick = {},
        )
    }
}

/**
 * A price long enough to trip the 18-character threshold that drops the price row to the smaller
 * text styles — the one branch in this item with two visual outcomes.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun OpenTradeListItem_LongPricePreview() {
    BisqTheme.Preview {
        OpenTradeListItem(
            item =
                createMockTradeItem(
                    quoteCurrencyCode = "COP",
                    formattedPrice = "1,234,567,890.00 COP",
                    formattedQuoteAmount = "12,345,678",
                ),
            userProfileIconProvider = previewUserProfileIconProvider,
            unreadCount = 0,
            onSelect = {},
            onPeerProfileClick = {},
        )
    }
}
