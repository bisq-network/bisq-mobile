package network.bisq.mobile.presentation.offerbook

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.displayString
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.RemoveOfferIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqVDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfile
import network.bisq.mobile.presentation.common.ui.theme.BisqModifier
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
@Composable
fun OfferCard(
    item: OfferItemPresentationModel,
    onSelectOffer: () -> Unit,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onPeerProfileClick: (String) -> Unit,
    enabled: Boolean = true,
    // Null = maker is not one of the user's contacts (card renders exactly as before). Non-null,
    // possibly blank = maker is a contact; blank falls back to a generic "Contact" label.
    contactTag: String? = null,
) {
    val userName by item.userName.collectAsState()
    val sellColor = BisqTheme.colors.danger.copy(alpha = 0.8f)
    val buyColor = BisqTheme.colors.primary.copy(alpha = 0.8f)
    val myOfferColor = BisqTheme.colors.mid_grey20
    val isMyOffer = item.isMyOffer

    val directionalLabel: String
    val directionalLabelColor: Color
    val makersDirection = item.bisqEasyOffer.direction
    val takersDirection = makersDirection.mirror

    if (isMyOffer) {
        directionalLabel = "mobile.bisqEasy.offerbook.offerCard.offerToBTC".i18n(makersDirection.displayString)
        directionalLabelColor = myOfferColor
    } else {
        if (takersDirection.isBuy) {
            directionalLabel = "mobile.bisqEasy.offerbook.offerCard.BuyBitcoinFrom".i18n(takersDirection.displayString)
            directionalLabelColor = buyColor
        } else {
            directionalLabel = "mobile.bisqEasy.offerbook.offerCard.SellBitcoinTo".i18n(takersDirection.displayString)
            directionalLabelColor = sellColor
        }
    }

    val myOfferBackgroundColor = BisqTheme.colors.primary.copy(alpha = 0.15f)
    val invalidOfferBackgroundColor = BisqTheme.colors.dark_grey50.copy(alpha = 0.4f)
    val backgroundColor =
        when {
            isMyOffer -> myOfferBackgroundColor
            item.isInvalidDueToReputation -> invalidOfferBackgroundColor
            else -> BisqTheme.colors.dark_grey50.copy(alpha = 0.9f)
        }

    // Contact cards grow one compact pill row (design PoC §5); non-contact cards keep the exact
    // 150dp height so they stay pixel-identical to before this feature.
    val height = if (contactTag != null) 150.dp + BisqUIConstants.ScreenPadding2X else 150.dp

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(8.dp))
                .background(color = backgroundColor)
                .height(height)
                .padding(BisqUIConstants.ScreenPadding)
                .debouncedClickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelectOffer,
                ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1.0F),
        ) {
            UserProfile(
                userProfile = item.makersUserProfile,
                userProfileIconProvider = userProfileIconProvider,
                reputation = item.makersReputationScore,
                supportedLanguageCodes = item.bisqEasyOffer.supportedLanguageCodes,
                showUserName = false,
                modifier = Modifier.fillMaxWidth(),
                // Only the avatar opens the maker's profile, as in the chat bubble. The rating and
                // languages below it stay part of the card's take-offer surface — tapping anywhere on a
                // card is how offers have always been taken, and a column-wide profile link would
                // swallow those taps. Null for own offers, which have no peer profile, and while the
                // card is disabled, so an in-flight take-offer cannot be interrupted by navigating away.
                onIconClick =
                    if (isMyOffer || !enabled) {
                        null
                    } else {
                        { onPeerProfileClick(item.makersUserProfile.id) }
                    },
            )

            if (contactTag != null) {
                BisqGap.VHalf()
                val pillText =
                    contactTag.takeIf { it.isNotBlank() }
                        ?: "mobile.bisqEasy.offerbook.offerCard.contact.genericLabel".i18n()
                ContactIndicatorPill(text = pillText)
            }
        }

        BisqGap.H1()
        BisqVDivider(thickness = BisqUIConstants.ScreenPaddingQuarter, color = BisqTheme.colors.primary)
        BisqGap.H1()

        Column(
            modifier = Modifier.weight(3.0F).fillMaxHeight(),
        ) {
            Row(
                modifier = Modifier.height(40.dp), // Fixed height to prevent pushing content down
            ) {
                if (isMyOffer) {
                    BisqText.BaseRegular(
                        text = directionalLabel,
                        color = directionalLabelColor,
                        singleLine = true,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                } else {
                    BisqText.BaseRegular(
                        text = directionalLabel,
                        color = directionalLabelColor,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )

                    BisqGap.HHalf()

                    AutoResizeText(
                        text = userName,
                        color = directionalLabelColor,
                        overflow = TextOverflow.Ellipsis,
                        textStyle = BisqTheme.typography.smallRegular,
                        maxLines = 2,
                        modifier =
                            BisqModifier
                                .textHighlight(
                                    BisqTheme.colors.dark_grey10
                                        .copy(alpha = 0.4f),
                                    BisqTheme.colors.mid_grey10,
                                ).padding(top = 4.dp, bottom = 2.dp)
                                .align(Alignment.CenterVertically),
                        minimumFontSize = 10.sp,
                    )
                }
            }

            BisqGap.VHalf()

            BisqText.BaseLight(item.formattedQuoteAmount)

            BisqGap.VHalf()

            val formattedPrice by item.formattedPrice.collectAsState()
            val priceDisplay =
                PriceSpecFormatter.formatPriceWithSpec(
                    formattedPrice,
                    item.bisqEasyOffer.priceSpec,
                )
            AutoResizeText(
                text = "@ $priceDisplay",
                textStyle = BisqTheme.typography.smallLight,
                maxLines = 1,
            )

            BisqGap.VHalf()
            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PaymentMethods(item.baseSidePaymentMethods, item.quoteSidePaymentMethods)

                if (isMyOffer) {
                    RemoveOfferIcon()
                }
            }

            BisqGap.VHalf()
        }
    }
}

/**
 * Neutral metadata pill signalling "this maker is one of my contacts" (#1792). Deliberately NOT
 * the green of `ContactCard`'s `ContactTagPill` and carrying no icon: on the offerbook green/red
 * are transactional (buy/sell/my-offer) and a checkmark would read as "verified", while this pill
 * is the viewing user's own private designation. See the merged design PoC
 * [network.bisq.mobile.presentation.design.contacts_offerbook.ContactOfferCard] §4.
 *
 * Background deviates from the PoC's `dark_grey50`: that shade was lifted from `ContactCard`'s
 * `ContactReasonPill`, which sits on an opaque `dark_grey40` directory card — one step lighter
 * than its surface. The offer card is instead translucent `dark_grey50` at 0.9 alpha, which
 * renders a `dark_grey50` pill virtually identical to the card beneath it. `mid_grey10`
 * re-establishes the one-step separation, restoring `ContactReasonPill`'s lighter-pill-on-
 * darker-card relationship.
 */
@Composable
private fun ContactIndicatorPill(text: String) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                .background(BisqTheme.colors.mid_grey10)
                .padding(horizontal = BisqUIConstants.ScreenPaddingHalf, vertical = BisqUIConstants.ScreenPaddingQuarter),
    ) {
        AutoResizeText(
            text = text,
            textStyle = BisqTheme.typography.xsmallMedium,
            color = BisqTheme.colors.light_grey20,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            minimumFontSize = 8.sp,
        )
    }
}

// Helper function to create a mock user profile icon provider for previews
private val previewUserProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { _ ->
    createEmptyImage()
}

// Helper function to create a mock offer item for previews
private fun createMockOfferItem(
    direction: DirectionEnum,
    isMyOffer: Boolean = false,
    isInvalidDueToReputation: Boolean = false,
    userName: String = "Satoshi",
    formattedQuoteAmount: String = "500 EUR",
    formattedPrice: String = "50,000",
    supportedLanguageCodes: List<String> = listOf("en"),
): OfferItemPresentationModel {
    val userProfile = createMockUserProfile(userName)
    val market = MarketVO("BTC", "EUR", "Bitcoin", "Euro")
    val amountSpec = QuoteSideFixedAmountSpecVO(500_00)
    val priceSpec = FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(50_000L, market))
    val makerNetworkId =
        NetworkIdVO(
            AddressByTransportTypeMapVO(mapOf()),
            PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"),
        )
    val bisqEasyOffer =
        BisqEasyOfferVO(
            id = "offer-123",
            date = 0L,
            makerNetworkId = makerNetworkId,
            direction = direction,
            market = market,
            amountSpec = amountSpec,
            priceSpec = priceSpec,
            protocolTypes = emptyList(),
            baseSidePaymentMethodSpecs = emptyList(),
            quoteSidePaymentMethodSpecs = emptyList(),
            offerOptions = emptyList(),
            supportedLanguageCodes = supportedLanguageCodes,
        )
    val reputationScore =
        ReputationScoreVO(
            totalScore = 1000L,
            fiveSystemScore = 5.0,
            ranking = 42,
        )
    val dto =
        OfferItemPresentationDto(
            bisqEasyOffer = bisqEasyOffer,
            isMyOffer = isMyOffer,
            userProfile = userProfile,
            formattedDate = "2024-01-15",
            formattedQuoteAmount = formattedQuoteAmount,
            formattedBaseAmount = "0.01 BTC",
            formattedPrice = formattedPrice,
            formattedPriceSpec = "Fix",
            quoteSidePaymentMethods = listOf("SEPA", "PayPal"),
            baseSidePaymentMethods = listOf("Bitcoin"),
            reputationScore = reputationScore,
        )
    return OfferItemPresentationModel(dto).apply {
        this.isInvalidDueToReputation = isInvalidDueToReputation
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_BuyPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL, // Maker sells, so taker buys
                    userName = "SatoshiNakamoto",
                    formattedQuoteAmount = "500 EUR",
                    formattedPrice = "50,000",
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_SellPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.BUY, // Maker buys, so taker sells
                    userName = "BitcoinTrader",
                    formattedQuoteAmount = "1,000 EUR",
                    formattedPrice = "52,000",
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_MyOfferBuyPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.BUY,
                    isMyOffer = true,
                    userName = "MyUser",
                    formattedQuoteAmount = "300 EUR",
                    formattedPrice = "49,000",
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_MyOfferSellPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL,
                    isMyOffer = true,
                    userName = "MyUser",
                    formattedQuoteAmount = "800 EUR",
                    formattedPrice = "51,000",
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_InvalidReputationPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL,
                    isInvalidDueToReputation = true,
                    userName = "LowReputationUser",
                    formattedQuoteAmount = "200 EUR",
                    formattedPrice = "48,000",
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_LongUserNamePreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL,
                    userName = "Max100CharMax100CharMax100CharMax100CharMax100CharMax100CharMax100CharMax100CharMax100CharMax100Char",
                    formattedQuoteAmount = "1,500 EUR",
                    formattedPrice = "53,000",
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_ManySupportedLanguageCodesPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL,
                    userName = "PolyglotTrader",
                    formattedQuoteAmount = "750 EUR",
                    formattedPrice = "50,500",
                    supportedLanguageCodes = listOf("en", "de", "es"),
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_ContactTaggedPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL, // Maker sells, so taker buys
                    userName = "TopSeller",
                    formattedQuoteAmount = "500 EUR",
                    formattedPrice = "50,000",
                ),
            contactTag = "Reliable SEPA trader",
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_ContactNoTagPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.BUY, // Maker buys, so taker sells
                    userName = "NewTrader0007",
                    formattedQuoteAmount = "150 EUR",
                    formattedPrice = "51,000",
                ),
            contactTag = "",
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_MyOfferManySupportedLanguageCodesPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.BUY,
                    isMyOffer = true,
                    userName = "MyPolyglotUser",
                    formattedQuoteAmount = "1,200 EUR",
                    formattedPrice = "49,800",
                    supportedLanguageCodes = listOf("en", "de", "es", "fr", "pt", "it", "nl", "tr", "pl", "ru"),
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}
