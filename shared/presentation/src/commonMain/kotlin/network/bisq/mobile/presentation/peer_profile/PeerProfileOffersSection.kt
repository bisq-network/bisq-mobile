package network.bisq.mobile.presentation.peer_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
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
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.common.ui.i18n.i18nText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * "Trade again" — the peer's own live offers, surfaced on their profile so acting on this specific
 * peer is a one-screen decision (design: `bisq-mobile` PR "designs/trade_again_with_contact").
 *
 * Renders nothing unless [PeerProfileUiState.showPeerOffersSection] — the section is
 * relationship-gated (traded-before OR contact). For a gated peer the header ALWAYS renders;
 * beneath it come the offer groups, the honest syncing row (client cold start), or an explicit
 * "no active offers" state — an absent section for a traded peer read as a missing feature in
 * field testing, so empty communicates instead of hiding.
 *
 * Row shape deliberately diverges from the offerbook's `OfferCard`: no per-row identity column
 * (every row belongs to the same, already-on-screen peer), which shrinks rows to a compact card
 * and makes many offers across many markets tractable inline. What IS shared with `OfferCard`,
 * to keep the two surfaces of one family: the direction-colored label and its i18n keys, the
 * `@ price` treatment via [PriceSpecFormatter], [PaymentMethods] icons, the muted background for
 * reputation-gated rows, and the rounded-card + `dark_grey50` language.
 */
@Composable
internal fun PeerProfileOffersSection(
    uiState: PeerProfileUiState,
    onAction: (PeerProfileUiAction) -> Unit,
) {
    if (!uiState.showPeerOffersSection) return

    val totalCount = uiState.peerOffersTotalCount
    Column(modifier = Modifier.fillMaxWidth()) {
        PeerOfferSectionHeader(
            peerDisplayName = uiState.displayName,
            hasTradedBefore = uiState.hasTradedBefore,
            totalCount = totalCount,
        )
        BisqGap.V1()
        when {
            uiState.peerOffers.isNotEmpty() -> {
                // Capped preview: the most recent offers across all markets, so a many-offer
                // peer cannot bury the action stack below the section. The rest live behind
                // the "View all" affordance on the dedicated peer-offers screen.
                uiState.peerOffersPreview.forEach { group ->
                    PeerOfferGroupHeader(marketCodes = group.marketCodes)
                    BisqGap.VHalf()
                    group.offers.forEach { offer ->
                        PeerOfferRow(
                            item = offer,
                            onClick = { onAction(PeerProfileUiAction.OnPeerOfferClick(offer.offerId)) },
                        )
                        BisqGap.VHalf()
                    }
                    BisqGap.VHalf()
                }
                if (totalCount > PeerProfileUiState.PEER_OFFERS_PREVIEW_CAP) {
                    PeerOffersViewAllRow(
                        totalCount = totalCount,
                        onClick = { onAction(PeerProfileUiAction.OnViewAllOffersClick) },
                    )
                }
            }

            // The client's all-markets cache can lag on a cold Tor start; silence here would
            // misreport "no offers" as a false negative, so say what is actually happening.
            uiState.isPeerOffersSyncing -> PeerOfferSyncingRow()

            else -> PeerOfferEmptyRow()
        }
    }
}

/**
 * Text-only primary-colored link row, mirroring the `ContactDetailsSection` "Edit" link style on
 * this same screen — no chevron (the codebase has no chevron convention to borrow).
 */
@Composable
private fun PeerOffersViewAllRow(
    totalCount: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .debouncedClickable(onClick = onClick)
                .padding(BisqUIConstants.ScreenPaddingHalf),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.SmallMedium(
            text = i18nText("mobile.peerProfile.offers.viewAll", totalCount),
            color = BisqTheme.colors.primary,
        )
    }
}

/**
 * "Trade again: {name}" once the user has actually traded with this peer; "Trade with {name}" —
 * deliberately no colon, it would read like a typo after the preposition — for a contact never
 * traded with. Names the action the section exists for; the offer count is demoted to a caption.
 */
@Composable
private fun PeerOfferSectionHeader(
    peerDisplayName: String,
    hasTradedBefore: Boolean,
    totalCount: Int,
) {
    val titleKey = if (hasTradedBefore) "mobile.peerProfile.offers.sectionTitle" else "mobile.peerProfile.offers.sectionTitleContactOnly"
    Column(modifier = Modifier.fillMaxWidth()) {
        AutoResizeText(
            text = i18nText(titleKey, peerDisplayName),
            textStyle = BisqTheme.typography.baseMedium,
            color = BisqTheme.colors.white,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            minimumFontSize = 11.sp,
        )
        // Zero offers carries no caption: the empty/syncing row below says it in words, and a
        // "0 active offers" line above it would state the same thing twice.
        if (totalCount > 0) {
            BisqGap.VQuarter()
            BisqText.XSmallRegularGrey(
                text =
                    if (totalCount == 1) {
                        "mobile.peerProfile.offers.countCaption.single".i18n()
                    } else {
                        i18nText("mobile.peerProfile.offers.countCaption", totalCount)
                    },
            )
        }
    }
}

/** Small caps market header ("BTC/EUR"), one per group, groups ordered newest-activity-first. */
@Composable
internal fun PeerOfferGroupHeader(marketCodes: String) {
    BisqText.XSmallRegularGrey(text = marketCodes.uppercase())
}

/** The gated-peer, zero-offers state: communicate the empty list instead of hiding the section. */
@Composable
internal fun PeerOfferEmptyRow() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey50.copy(alpha = 0.6f))
                .padding(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.SmallRegularGrey(text = "mobile.peerProfile.offers.empty".i18n())
    }
}

@Composable
internal fun PeerOfferSyncingRow() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey50.copy(alpha = 0.6f))
                .padding(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = BisqTheme.colors.mid_grey20,
        )
        BisqText.SmallRegularGrey(text = "mobile.peerProfile.offers.syncing".i18n())
    }
}

/**
 * Compact row for one of the peer's offers. The whole row is the tap target (same no-added-chrome
 * convention as the offerbook card); the presenter routes the tap through the shared eligibility
 * gate, so a reputation-gated row opens the requirement dialog instead of the wizard. Gated rows
 * additionally carry an inline reason caption — this screen is about one peer, so the extra line
 * costs little and explains what would otherwise look like a randomly greyed-out row.
 */
@Composable
internal fun PeerOfferRow(
    item: OfferItemPresentationModel,
    onClick: () -> Unit,
) {
    val takersDirection = item.bisqEasyOffer.direction.mirror
    val directionColor =
        if (takersDirection.isBuy) {
            BisqTheme.colors.primary.copy(alpha = 0.8f)
        } else {
            BisqTheme.colors.danger.copy(alpha = 0.8f)
        }
    val directionalLabel =
        if (takersDirection.isBuy) {
            "mobile.bisqEasy.offerbook.offerCard.BuyBitcoinFrom".i18n(takersDirection.displayString)
        } else {
            "mobile.bisqEasy.offerbook.offerCard.SellBitcoinTo".i18n(takersDirection.displayString)
        }
    val isTakeable = !item.isInvalidDueToReputation
    val backgroundColor =
        if (isTakeable) {
            BisqTheme.colors.dark_grey50.copy(alpha = 0.9f)
        } else {
            BisqTheme.colors.dark_grey50.copy(alpha = 0.4f)
        }
    val userName by item.userName.collectAsState()
    val formattedPrice by item.formattedPrice.collectAsState()
    val priceDisplay = PriceSpecFormatter.formatPriceWithSpec(formattedPrice, item.bisqEasyOffer.priceSpec)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(backgroundColor)
                .debouncedClickable(onClick = onClick)
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BisqText.BaseRegular(
                text = "$directionalLabel $userName",
                color = directionColor,
                modifier = Modifier.weight(1f, fill = false),
            )
            BisqGap.HHalf()
            AutoResizeText(
                text = "@ $priceDisplay",
                textStyle = BisqTheme.typography.smallLight,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                minimumFontSize = 9.sp,
            )
        }
        BisqGap.VHalf()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BisqText.BaseLight(text = item.formattedQuoteAmount)
            PaymentMethods(item.baseSidePaymentMethods, item.quoteSidePaymentMethods)
        }
        if (!isTakeable) {
            BisqGap.VHalf()
            BisqText.XSmallRegularGrey(text = "mobile.peerProfile.offers.notTakeableReason".i18n())
        }
    }
}

// ============================================================================================
// Previews
// ============================================================================================

@ExcludeFromCoverage
private fun previewOffer(
    id: String,
    market: MarketVO,
    date: Long,
    direction: DirectionEnum = DirectionEnum.SELL,
    amount: String = "100.00 - 500.00 ${market.quoteCurrencyCode}",
    invalidDueToReputation: Boolean = false,
): OfferItemPresentationModel {
    val makerNetworkId =
        NetworkIdVO(
            AddressByTransportTypeMapVO(mapOf()),
            PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "SatoshiFan"),
        )
    val offer =
        BisqEasyOfferVO(
            id = id,
            date = date,
            makerNetworkId = makerNetworkId,
            direction = direction,
            market = market,
            amountSpec = QuoteSideFixedAmountSpecVO(500_000L),
            priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_000_00L, market) }),
            protocolTypes = emptyList(),
            baseSidePaymentMethodSpecs = emptyList(),
            quoteSidePaymentMethodSpecs = emptyList(),
            offerOptions = emptyList(),
            supportedLanguageCodes = emptyList(),
        )
    return OfferItemPresentationModel(
        OfferItemPresentationDto(
            bisqEasyOffer = offer,
            isMyOffer = false,
            userProfile = createMockUserProfile("SatoshiFan"),
            formattedDate = "",
            formattedQuoteAmount = amount,
            formattedBaseAmount = "",
            formattedPrice = "50,000 ${market.quoteCurrencyCode}",
            formattedPriceSpec = "",
            quoteSidePaymentMethods = listOf("SEPA"),
            baseSidePaymentMethods = listOf("MAIN_CHAIN"),
            reputationScore = ReputationScoreVO(0, 0.0, 0),
        ),
    ).also { it.isInvalidDueToReputation = invalidDueToReputation }
}

@ExcludeFromCoverage
private fun previewState(hasTradedBefore: Boolean = true) =
    PeerProfileUiState(
        userProfile = createMockUserProfile("SatoshiFan"),
        displayName = "SatoshiFan",
        isLoading = false,
        hasTradedBefore = hasTradedBefore,
        isContact = !hasTradedBefore,
        peerOffers =
            listOf(
                PeerOffersMarketGroupUiState(
                    marketCodes = "BTC/EUR",
                    offers =
                        listOf(
                            previewOffer("o1", MarketVO("BTC", "EUR", "Bitcoin", "Euro"), date = 9_000L),
                            previewOffer(
                                "o2",
                                MarketVO("BTC", "EUR", "Bitcoin", "Euro"),
                                date = 5_000L,
                                direction = DirectionEnum.BUY,
                                amount = "5,000.00 EUR",
                                invalidDueToReputation = true,
                            ),
                        ),
                ),
                PeerOffersMarketGroupUiState(
                    marketCodes = "BTC/USD",
                    offers = listOf(previewOffer("o3", MarketVO("BTC", "USD", "Bitcoin", "US Dollar"), date = 1_000L)),
                ),
            ),
    )

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileOffersSection_TradedBeforePreview() {
    BisqTheme.Preview {
        PeerProfileOffersSection(uiState = previewState(hasTradedBefore = true), onAction = {})
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileOffersSection_ContactOnlyPreview() {
    BisqTheme.Preview {
        PeerProfileOffersSection(uiState = previewState(hasTradedBefore = false), onAction = {})
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileOffersSection_SyncingPreview() {
    BisqTheme.Preview {
        PeerProfileOffersSection(
            uiState = previewState().copy(peerOffers = emptyList(), isPeerOffersSyncing = true),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun PeerProfileOffersSection_EmptyPreview() {
    BisqTheme.Preview {
        PeerProfileOffersSection(
            uiState = previewState().copy(peerOffers = emptyList(), isPeerOffersSyncing = false),
            onAction = {},
        )
    }
}
