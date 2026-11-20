package network.bisq.mobile.presentation.offerbook

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import network.bisq.mobile.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVO
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
import network.bisq.mobile.domain.formatters.OfferAgeFormatter
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.RemoveOfferIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIconLightGrey
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqVDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfile
import network.bisq.mobile.presentation.common.ui.theme.BisqModifier
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

const val OFFER_CARD_PRICE_TAG = "offerCard.price"
const val OFFER_CARD_AGE_TAG = "offerCard.age"

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
    isBelowOwnReputation: Boolean = false,
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

    // Named so the right column (below) can stay pinned to a non-contact card's content height no
    // matter how tall the whole row grows for the contact pill.
    val baseCardHeight = 150.dp
    val baseContentHeight = baseCardHeight - (BisqUIConstants.ScreenPadding * 2)

    // Contact cards grow one compact pill row (design PoC §5); non-contact cards keep the exact
    // 150dp height so they stay pixel-identical to before this feature.
    val height =
        if (contactTag != null) {
            baseCardHeight + BisqUIConstants.ScreenPadding2X + BisqUIConstants.ScreenPaddingHalf
        } else {
            baseCardHeight
        }

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
        // Only the avatar opens the maker's profile, as in the chat bubble. The rating and
        // languages below it stay part of the card's take-offer surface — tapping anywhere on a
        // card is how offers have always been taken, and a column-wide profile link would
        // swallow those taps. Null for own offers, which have no peer profile, and while the
        // card is disabled, so an in-flight take-offer cannot be interrupted by navigating away.
        val onMakerIconClick =
            if (isMyOffer || !enabled) {
                null
            } else {
                { onPeerProfileClick(item.makersUserProfile.id) }
            }

        if (contactTag != null) {
            // Contact cards grow one compact pill row below the languages row (design PoC §5).
            // The pill is scoped to THIS column only — the right column below keeps
            // baseContentHeight (a non-contact card's exact content height), so its rows never
            // shift regardless of how tall this column gets.
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
                    onIconClick = onMakerIconClick,
                )

                BisqGap.V1()
                val pillText =
                    contactTag.takeIf { it.isNotBlank() }
                        ?: "mobile.bisqEasy.offerbook.offerCard.contact.genericLabel".i18n()
                ContactIndicatorPill(text = pillText)
            }
        } else {
            // Exactly the layout from before this feature: no wrapper column, no extra
            // alignment — pixel-identical to main.
            UserProfile(
                userProfile = item.makersUserProfile,
                userProfileIconProvider = userProfileIconProvider,
                reputation = item.makersReputationScore,
                supportedLanguageCodes = item.bisqEasyOffer.supportedLanguageCodes,
                showUserName = false,
                modifier = Modifier.weight(1.0F),
                onIconClick = onMakerIconClick,
            )
        }

        BisqGap.H1()
        BisqVDivider(thickness = BisqUIConstants.ScreenPaddingQuarter, color = BisqTheme.colors.primary)
        BisqGap.H1()

        Column(
            modifier = Modifier.weight(3.0F).height(baseContentHeight),
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
                PriceSpecFormatter.formatPriceWithTypeLabel(
                    formattedPrice,
                    item.bisqEasyOffer.priceSpec,
                )
            AutoResizeText(
                text = "@ $priceDisplay",
                textStyle = BisqTheme.typography.smallLight,
                maxLines = 1,
                modifier = Modifier.testTag(OFFER_CARD_PRICE_TAG),
            )

            BisqGap.VHalf()
            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PaymentMethods(item.baseSidePaymentMethods, item.quoteSidePaymentMethods)

                // Weighted, so it only gets the width the icons leave over. Its gaps are padding
                // inside that share, so they yield with it instead of squeezing the remove icon.
                OfferAgeLabel(
                    createdAtMillis = item.bisqEasyOffer.date,
                    modifier =
                        Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                            .padding(
                                start = BisqUIConstants.ScreenPadding,
                                end = if (isMyOffer) BisqUIConstants.ScreenPaddingHalf else 0.dp,
                            ),
                )

                if (isMyOffer) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isBelowOwnReputation) {
                            BelowReputationBadge()
                        }
                        RemoveOfferIcon()
                    }
                }
            }

            BisqGap.VHalf()
        }
    }
}

/**
 * Neutral metadata pill signalling "this maker is one of my contacts". Deliberately NOT
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

/**
 * Low-contrast "Today" / "N days" at the right of the payment-methods row. It shrinks from 12 to
 * 10sp and is left out when even that does not fit, as a cut-off number is worse than none.
 */
@Composable
private fun OfferAgeLabel(
    createdAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    val age = OfferAgeFormatter.formatAgeInDays(createdAtMillis)
    val description = "mobile.bisqEasy.offerbook.offerCard.age.a11y".i18n(age)
    AutoResizeText(
        text = age,
        color = BisqTheme.colors.mid_grey30,
        textStyle = BisqTheme.typography.xsmallRegular,
        textAlign = TextAlign.End,
        minimumFontSize = 10.sp,
        hideWhenOverflowingAtMinimum = true,
        modifier =
            modifier
                .testTag(OFFER_CARD_AGE_TAG)
                .semantics { contentDescription = description },
    )
}

/** Marks my offer as below my reputation, next to its delete icon. */
@Composable
private fun BelowReputationBadge() {
    val description = "mobile.bisqEasy.offerbook.offersBelowReputation.badge.contentDescription".i18n()
    WarningIconLightGrey(
        modifier =
            Modifier
                .size(BisqUIConstants.ScreenPadding2X)
                .clearAndSetSemantics { contentDescription = description },
    )
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
    priceSpec: PriceSpecVO? = null,
    ageInDays: Int = 3,
    quoteSidePaymentMethods: List<String> = listOf("SEPA", "PayPal"),
    baseSidePaymentMethods: List<String> = listOf("Bitcoin"),
): OfferItemPresentationModel {
    val userProfile = createMockUserProfile(userName)
    val market = MarketVO("BTC", "EUR", "Bitcoin", "Euro")
    val amountSpec = QuoteSideFixedAmountSpecVO(500_00)
    val offerPriceSpec = priceSpec ?: FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(50_000L, market))
    val makerNetworkId =
        NetworkIdVO(
            AddressByTransportTypeMapVO(mapOf()),
            PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"),
        )
    val bisqEasyOffer =
        BisqEasyOfferVO(
            id = "offer-123",
            date = DateUtils.now() - ageInDays * 86_400_000L,
            makerNetworkId = makerNetworkId,
            direction = direction,
            market = market,
            amountSpec = amountSpec,
            priceSpec = offerPriceSpec,
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
            quoteSidePaymentMethods = quoteSidePaymentMethods,
            baseSidePaymentMethods = baseSidePaymentMethods,
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
private fun OfferCard_MyOfferBelowReputationPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL,
                    isMyOffer = true,
                    userName = "MyUser",
                    formattedQuoteAmount = "2,000 EUR",
                    formattedPrice = "51,000",
                ),
            isBelowOwnReputation = true,
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

private val crowdedQuoteSidePaymentMethods = listOf("SEPA", "REVOLUT", "WISE", "ZELLE")
private val crowdedBaseSidePaymentMethods = listOf("MAIN_CHAIN", "LN")

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_FloatAboveMarketTodayPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL,
                    formattedPrice = "51,000",
                    priceSpec = FloatPriceSpecVO(0.02),
                    ageInDays = 0,
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
private fun OfferCard_FloatBelowMarketOneDayPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.BUY,
                    formattedPrice = "48,750",
                    priceSpec = FloatPriceSpecVO(-0.025),
                    ageInDays = 1,
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
private fun OfferCard_MarketPriceOldOfferPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL,
                    formattedPrice = "50,000",
                    priceSpec = MarketPriceSpecVO(),
                    ageInDays = 812,
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

// An offset this small rounds to 0.00 %, so it must read "Market price", not "+0.00 % vs market".
@ExcludeFromCoverage
@Preview
@Composable
private fun OfferCard_FloatRoundingToMarketPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL,
                    formattedPrice = "50,001",
                    priceSpec = FloatPriceSpecVO(0.00001),
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
private fun OfferCard_CrowdedPaymentMethodsPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL,
                    priceSpec = FloatPriceSpecVO(-0.0125),
                    ageInDays = 812,
                    quoteSidePaymentMethods = crowdedQuoteSidePaymentMethods,
                    baseSidePaymentMethods = crowdedBaseSidePaymentMethods,
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

// The age label should shrink, then disappear, while the remove icon and badge keep their size.
@ExcludeFromCoverage
@Preview(widthDp = 320)
@Preview(widthDp = 320, fontScale = 1.5f)
@Preview(widthDp = 320, fontScale = 2f)
@Composable
private fun OfferCard_MyOfferCrowdedNarrowPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.BUY,
                    isMyOffer = true,
                    formattedQuoteAmount = "1,200 EUR",
                    priceSpec = FloatPriceSpecVO(0.0375),
                    ageInDays = 812,
                    quoteSidePaymentMethods = crowdedQuoteSidePaymentMethods,
                    baseSidePaymentMethods = crowdedBaseSidePaymentMethods,
                ),
            isBelowOwnReputation = true,
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(widthDp = 320, fontScale = 1.5f)
@Preview(widthDp = 320, fontScale = 2f)
@Composable
private fun OfferCard_PeerOfferNarrowLargeFontPreview() {
    BisqTheme.Preview {
        OfferCard(
            item =
                createMockOfferItem(
                    direction = DirectionEnum.SELL,
                    userName = "SatoshiNakamoto",
                    priceSpec = FloatPriceSpecVO(-0.0125),
                    ageInDays = 812,
                    quoteSidePaymentMethods = crowdedQuoteSidePaymentMethods,
                    baseSidePaymentMethods = crowdedBaseSidePaymentMethods,
                ),
            onSelectOffer = {},
            userProfileIconProvider = previewUserProfileIconProvider,
            onPeerProfileClick = {},
        )
    }
}
