package network.bisq.mobile.presentation.design.offer_card_price_age

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.LanguageIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqVDivider
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.design.trade_terms.SIMULATED_PRICE_DETAIL_FIXED
import network.bisq.mobile.presentation.design.trade_terms.SIMULATED_PRICE_DETAIL_FLOAT_ABOVE
import network.bisq.mobile.presentation.design.trade_terms.SIMULATED_PRICE_DETAIL_FLOAT_BELOW
import network.bisq.mobile.presentation.design.trade_terms.SIMULATED_PRICE_DETAIL_MARKET
import network.bisq.mobile.presentation.design.trade_terms.SimulatedTakeOfferReview
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Offer card: explicit price wording and offer age. Also the price wording on the take-offer
 * review step.
 *
 * Design preview file. Open it in Android Studio's Split / Design view and review the
 * `@Preview` functions. The restaged card is built from primitives only and is not wired to a
 * presenter, Koin or a domain type.
 *
 * ---------------------------------------------------------------------------------------------
 * ## 1. Price wording
 *
 * Today the card shows `@ 50,000` for a fixed price and `@ 51,000 (+2.00 %)` for a floating one.
 * A fixed price carries no label at all, and a floating price has a bare percentage that does not
 * say what it is relative to. Rule: the price line ALWAYS ends in a parenthesised price type.
 *
 * | Price spec                                 | Card price line                         | Review step line under the price |
 * |--------------------------------------------|-----------------------------------------|----------------------------------|
 * | `FixPriceSpecVO`                           | `@ 50,000 (Fixed)`                      | `Fixed`                          |
 * | `FloatPriceSpecVO`, percentage > 0         | `@ 51,000 (+2.00 % vs market)`          | `+2.00 % vs market`              |
 * | `FloatPriceSpecVO`, percentage < 0         | `@ 49,000 (-2.00 % vs market)`          | `-2.00 % vs market`              |
 * | `FloatPriceSpecVO`, rounds to 0.00 %       | `@ 50,000 (Market price)`               | `Market price`                   |
 * | `MarketPriceSpecVO`                        | `@ 50,000 (Market price)`               | `Market price`                   |
 *
 * Formatter contract (domain, `PriceSpecFormatter`):
 * - Add `formatPriceTypeLabel(priceSpec): String` returning the right-hand column above (the
 *   label without parentheses). `formatPriceWithSpec(formattedPrice, priceSpec)` becomes
 *   `"$formattedPrice (${formatPriceTypeLabel(priceSpec)})"` for every spec, including fixed.
 * - Do NOT change the `abbreviated` keys used by `getFormattedPriceSpec`. They are shared with the
 *   open-trades list, whose wording is out of scope.
 * - The percentage is `PercentageFormatter.format(abs(percentage))`, so it keeps the formatter's
 *   two decimals and the space before the percent sign.
 * - The sign is decided AFTER rounding to the formatter's precision. A percentage of -0.004 %
 *   rounds to 0.00 % and must read "Market price", never "-0.00 % vs market" or "+0.00 % vs
 *   market". See [simulatedPriceTypeLabel] for the reference behaviour.
 * - The percentage is the maker's configured offset (`FloatPriceSpecVO.percentage`), not the
 *   live difference to the current market price. The card shows what the maker chose.
 * - The card still passes the already-formatted price string in; the percentage stays in the spec.
 *
 * Why parentheses and not a separate chip: the price line is a single `AutoResizeText` (14sp
 * shrinking to 10sp, one line). Keeping one text keeps the line's height fixed, and the shrink
 * behaviour already handles long prices and long translations. A chip or a second line would add
 * height, see section 3. Why "vs market" and not "above/below market price": it is shorter, so it
 * survives at 12 to 14sp on a 360dp phone, and the sign carries the direction.
 *
 * Review step: the price block already has the price, the currency pair and a long grey detail
 * line ("Float price. 2.00 % above market price of 45,000 USD/BTC"). The new type label is a
 * `SmallRegular` line in `light_grey20` directly under the price row and above the grey detail
 * line, so the first thing next to the resulting price is the same words as the card. The grey
 * detail line is kept unchanged because it also carries the market reference price, which the
 * label does not.
 *
 * ## 2. Offer age
 *
 * ### Computation
 * Same as the desktop offerbook (`TimeFormatter.formatAgeInDays`), from the offer's creation
 * date (`BisqEasyOfferVO.date`, epoch millis):
 *
 * | Condition                                      | Result                                                    |
 * |------------------------------------------------|-----------------------------------------------------------|
 * | `days = (now - date) / 86_400_000`, truncated  | Elapsed 24-hour periods, not calendar days                 |
 * | `days == 0` (under 24 h old)                   | `temporal.today` = "Today"                                 |
 * | `days == 1`                                    | `temporal.day` plural form `1` = "1 day"                   |
 * | `days >= 2`                                    | `temporal.day` plural form `*` = "N days"                  |
 * | `date` in the future (clock skew)              | `days` clamped to 0, so "Today". Never a negative number   |
 * | `date <= 0` or absurdly old                    | Same arithmetic. Clamp the result to the range of `Int`    |
 *
 * Elapsed 24-hour periods (not "created yesterday") is deliberate: it matches desktop, needs no
 * time zone, and cannot disagree between two phones. An offer created at 23:50 reads "Today" at
 * 00:10 the next day. Use the injectable `DateUtils` clock so the formatter is testable. Do not
 * reuse `DateUtils.periodFrom`, it counts UTC calendar days, which differs from desktop.
 *
 * The label is computed when the card is composed and not re-evaluated on a timer; a card that
 * stays on screen across a day boundary keeps its text until the next recomposition. Desktop
 * behaves the same. Large day counts stay as day counts ("812 days"), exactly like desktop's
 * `formatAgeInDays`; years are not introduced.
 *
 * ### Placement and the card height
 *
 * The age lives in the existing payment-methods row at the bottom of the right column, at its
 * right edge. This costs ZERO height on every card:
 *
 * - The right column is pinned to 126dp (150dp card minus 12dp padding top and bottom). Its rows
 *   are: header 40dp, 6dp gap, amount about 22dp, 6dp gap, price about 19dp, 6dp gap, a flexible
 *   spacer, payment-methods row 20dp, 6dp gap. That is about 125dp used, about 1 to 2dp slack. No
 *   whole extra text line fits without growing the card.
 * - The payment-methods row is 20dp high and a 12sp line is about 16dp high, so the age fits
 *   inside the row that already exists. It is vertically centred in it.
 * - The payment icons occupy the left part of that row, and for a peer's offer the right part is
 *   empty today. For the user's own offer the right edge holds the remove icon; the age sits
 *   directly to its left.
 * - Rows above the payment row are untouched, so the amount stays at the same y on every card in
 *   a mixed list, and the card stays 150dp (contact cards stay at their 168dp).
 *
 * Space priority inside the row: the payment icons are measured first and keep their full width.
 * The age gets what is left, after a 12dp gap to the icons, and steps down in three stages:
 * 12sp, then 10sp, then omitted for that card. It is never wrapped and never cut into a fragment
 * such as "112 d...", because a truncated number is worse than no number. On a 360dp phone the
 * right column is about 230dp wide:
 * - 2 + 1 methods (the common case) leave about 110dp: "Today" and "812 days" fit at 12sp.
 * - 4 + 2 methods leave about 40dp: "Today" fits at 12sp, "112 days" needs 10sp and only just
 *   fits or is omitted, depending on the font.
 * - 5 + 3 methods fill the whole column (this already holds for the icons alone), so the age is
 *   omitted on that card.
 * - The own-offer card gives up another 32dp to the remove icon, so it reaches those stages
 *   sooner.
 * The omission is a deliberate, rare degradation. Implement it by rendering the age at 12sp,
 * re-rendering at 10sp when `TextLayoutResult.hasVisualOverflow` is true, and composing nothing
 * when it still overflows; key the state on the available width so it is recomputed on rotation.
 * See [OfferAgeLabel] and the crowded-row previews.

 * Style: `xsmallRegular` (12sp), colour `mid_grey30`, right aligned, one line. Roughly 5.5:1
 * against the card background, which is above WCAG AA (4.5:1) and still clearly a tertiary
 * detail next to the white amount and price. `mid_grey20` (the usual "greyish text" token) is only
 * about 3:1 on this card, so it is NOT used.
 *
 * Alternative evaluated: the age on its own line under the price. It costs one text line of
 * about 16dp, minus the 2dp slack, so the card grows from 150dp to about 164dp (contact cards
 * from 168dp to about 182dp), and every card in the list would grow, not just the ones that carry
 * new information. See [AgePlacement.OWN_LINE] and the comparison preview. Not recommended: a
 * list of dozens of cards loses about 9% of its density for a tertiary fact.
 *
 * Alternative rejected: age at the right of the amount row. The amount row has no spare width for
 * ranges such as "100,000 - 1,000,000 JPY", which already fill the column.
 *
 * Related follow-up, not part of this design: the already-planned rebalancing of the vertical
 * gaps (header to amount 6 to 3dp, amount to price 6 to 3dp, price to payments 6 to 12dp, zero
 * net height) keeps the payment-methods row at the same y within 0 net dp, so the age placement
 * stays valid after it lands.
 *
 * ### Which surfaces
 * - `OfferCard` in the offerbook, for both peers' offers and the user's own offers.
 * - The take-offer review step does not show the age; it shows the price wording only.
 * - TODO: `PeerOfferRow` on the peer profile uses the same price formatter and so gets the new
 *   price wording automatically. Decide separately whether its compact row also shows the age;
 *   desktop's profile-card offers table has an age column, but the row has no free metadata
 *   slot to host it without a layout change.
 *
 * ## 3. Behaviour table (card)
 *
 * | Case                    | Price line                           | Age (bottom right)         | Card height |
 * |-------------------------|--------------------------------------|----------------------------|-------------|
 * | Peer offer, fixed       | `@ 50,000 (Fixed)`                   | "Today" / "N days"         | 150dp       |
 * | Peer offer, float +     | `@ 51,000 (+2.00 % vs market)`       | same                       | 150dp       |
 * | Peer offer, float -     | `@ 49,000 (-2.00 % vs market)`       | same                       | 150dp       |
 * | Own offer               | same rules                           | left of the remove icon    | 150dp       |
 * | Contact card            | same rules                           | same                       | 168dp       |
 * | Invalid-due-to-reputation | same rules                         | same, no extra dimming     | 150dp       |
 * | Long price or locale    | shrinks to 10sp, never wraps         | unaffected                 | 150dp       |
 * | Crowded payment row     | unaffected                           | 10sp, then omitted         | 150dp       |
 *
 * ## 4. Accessibility
 *
 * - The card is a single tap target that merges its children. Reading order inside it follows
 *   the layout: maker, direction and name, amount, price line (with its type), payment methods,
 *   then the age.
 * - The age text node carries `contentDescription` "Offer age: Today" / "Offer age: 3 days" so a
 *   bare "3 days" is not read out of context. The visible text stays "3 days".
 * - The price type is part of the same string as the price, so it is read as one phrase.
 * - Test tags: `offerCard.price`, `offerCard.age`.
 * - Font scale: the card height is fixed at 150dp today; at large font scales the price line
 *   already shrinks first and the age shrinks to 10sp. Behaviour at 200% is unchanged by this
 *   design (see the large-font preview); making the card height scale with font size is a
 *   separate concern.
 *
 * ## 5. i18n
 *
 * Reused (already translated): `temporal.today` = "Today"; `temporal.day.1` = "{0} day";
 * `temporal.day.*` = "{0} days"; `bisqEasy.tradeWizard.review.chatMessage.marketPrice` =
 * "Market price". Plurals go through `i18nPlural("temporal.day", days)`.
 *
 * New, in `shared/domain/src/commonMain/resources/mobile/mobile.properties` only, English only:
 * - `mobile.bisqEasy.offerbook.offerCard.price.fixed` = Fixed
 * - `mobile.bisqEasy.offerbook.offerCard.price.floatAbove` = +{0} vs market
 * - `mobile.bisqEasy.offerbook.offerCard.price.floatBelow` = -{0} vs market
 * - `mobile.bisqEasy.offerbook.offerCard.age.a11y` = Offer age: {0}
 *
 * `{0}` is `PercentageFormatter.format(abs(percentage))`, for example "2.00 %".
 *
 * ## 6. Tests to add
 *
 * - `PriceSpecFormatterTest`, `formatPriceTypeLabel` and `formatPriceWithSpec`: fixed gives
 *   "Fixed" and "50,000 (Fixed)"; float +2 % gives "+2.00 % vs market"; float -2 % gives
 *   "-2.00 % vs market"; float 0.0 and -0.00004 give "Market price"; market price spec gives
 *   "Market price". The existing `formatPriceWithSpec` fixed-price test changes: it no longer
 *   returns the bare price.
 * - A new offer-age formatter test (domain, with the injectable clock): 0 ms old, 23 h 59 min,
 *   exactly 24 h, 47 h 59 min, 48 h, 812 days, and a date one day in the future, expecting
 *   "Today", "Today", "1 day", "1 day", "2 days", "812 days", "Today".
 * - Presentation: `TakeOfferReviewPresenter` exposes the type label for fixed, float +, float -
 *   and market specs.
 * - UI: `OfferCard` shows `offerCard.age` for a peer offer and for an own offer, and
 *   `offerCard.price` contains the type label for fixed and float offers. Card height assertion:
 *   a regular card still measures 150dp with the age present.
 *
 * TODO: Confirm the on-screen length of `price.floatAbove` in the longest supported translations
 * once translated; if it does not stay within the 10sp floor at 360dp, shorten the translation
 * rather than adding a second line.
 */
internal enum class AgePlacement {
    /** Recommended: inside the existing payment-methods row. Zero height cost. */
    PAYMENTS_ROW,

    /** Evaluated alternative: its own line under the price. Grows the card by about 14dp. */
    OWN_LINE,

    /** No age at all: the card as it is today, for comparison. */
    NONE,
}

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Reference behaviour of the price-type label for the previews. The production formatter must
 * match it. [fraction] is the maker's offset as a fraction (0.02 = +2 %), or null for a fixed
 * price.
 */
internal fun simulatedPriceTypeLabel(fraction: Double?): String {
    if (fraction == null) return "Fixed"
    val hundredths = (abs(fraction) * 10_000).roundToLong()
    if (hundredths == 0L) return "Market price"
    val whole = hundredths / 100
    val decimals = (hundredths % 100).toString().padStart(2, '0')
    val sign = if (fraction > 0) "+" else "-"
    return "$sign$whole.$decimals % vs market"
}

/** Reference behaviour of the age computation: elapsed 24-hour periods, never negative. */
internal fun simulatedOfferAgeInDays(
    offerDateMillis: Long,
    nowMillis: Long,
): Long = ((nowMillis - offerDateMillis) / MILLIS_PER_DAY).coerceAtLeast(0L)

/** Reference label for the previews: "Today", "1 day", "N days". */
internal fun simulatedOfferAgeLabel(ageInDays: Long): String =
    when (ageInDays) {
        0L -> "Today"
        1L -> "1 day"
        else -> "$ageInDays days"
    }

private fun priceLine(
    price: String,
    fraction: Double?,
) = "@ $price (${simulatedPriceTypeLabel(fraction)})"

/**
 * Restage of the offerbook card with the new price line and the age. Layout constants mirror
 * the production card: 150dp (168dp for a contact), 12dp padding, left identity column (weight
 * 1), 3dp divider, right column (weight 3) pinned to the content height of a regular card.
 */
@ExcludeFromCoverage
@Composable
internal fun SimulatedOfferCard(
    directionLabel: String,
    isBuyDirection: Boolean,
    isMyOffer: Boolean,
    userName: String,
    quoteAmountLabel: String,
    priceText: String,
    ageLabel: String,
    ageAccessibilityLabel: String,
    quotePaymentCount: Int,
    basePaymentCount: Int,
    agePlacement: AgePlacement,
    modifier: Modifier = Modifier,
    contactTag: String? = null,
    starRating: Double = 4.5,
    languageCodesLabel: String = "EN",
) {
    val directionColor =
        if (isBuyDirection) {
            BisqTheme.colors.primary.copy(alpha = 0.8f)
        } else {
            BisqTheme.colors.danger.copy(alpha = 0.8f)
        }
    val backgroundColor =
        if (isMyOffer) {
            BisqTheme.colors.primary.copy(alpha = 0.15f)
        } else {
            BisqTheme.colors.dark_grey50.copy(alpha = 0.9f)
        }
    val baseCardHeight = 150.dp
    val baseContentHeight = baseCardHeight - (BisqUIConstants.ScreenPadding * 2)
    val ownLineExtra = if (agePlacement == AgePlacement.OWN_LINE) 14.dp else 0.dp
    val contactExtra = if (contactTag != null) BisqUIConstants.ScreenPadding2X + BisqUIConstants.ScreenPaddingHalf else 0.dp
    val cardHeight = baseCardHeight + contactExtra + ownLineExtra

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .height(cardHeight)
                .padding(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(BisqUIConstants.ScreenPadding4X)
                        .clip(CircleShape)
                        .background(BisqTheme.colors.mid_grey10),
            )
            BisqGap.V1()
            StarRating(rating = starRating)
            BisqGap.V2()
            Row(verticalAlignment = Alignment.CenterVertically) {
                LanguageIcon()
                BisqText.SmallRegularGrey(" : ")
                AutoResizeText(
                    text = languageCodesLabel,
                    overflow = TextOverflow.Ellipsis,
                    textStyle = BisqTheme.typography.smallRegular,
                    maxLines = 1,
                    minimumFontSize = 10.sp,
                )
            }
            if (contactTag != null) {
                BisqGap.V1()
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                            .background(BisqTheme.colors.mid_grey10)
                            .padding(
                                horizontal = BisqUIConstants.ScreenPaddingHalf,
                                vertical = BisqUIConstants.ScreenPaddingQuarter,
                            ),
                ) {
                    BisqText.XSmallMedium(text = contactTag, color = BisqTheme.colors.light_grey20)
                }
            }
        }

        BisqGap.H1()
        BisqVDivider(thickness = BisqUIConstants.ScreenPaddingQuarter, color = BisqTheme.colors.primary)
        BisqGap.H1()

        Column(modifier = Modifier.weight(3f).height(baseContentHeight + ownLineExtra)) {
            Row(modifier = Modifier.height(40.dp)) {
                BisqText.BaseRegular(
                    text = directionLabel,
                    color = if (isMyOffer) BisqTheme.colors.mid_grey20 else directionColor,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                if (!isMyOffer) {
                    BisqGap.HHalf()
                    AutoResizeText(
                        text = userName,
                        color = directionColor,
                        overflow = TextOverflow.Ellipsis,
                        textStyle = BisqTheme.typography.smallRegular,
                        maxLines = 2,
                        minimumFontSize = 10.sp,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                                .background(BisqTheme.colors.dark_grey10.copy(alpha = 0.4f))
                                .padding(horizontal = BisqUIConstants.ScreenPaddingHalf, vertical = 2.dp)
                                .align(Alignment.CenterVertically),
                    )
                }
            }

            BisqGap.VHalf()
            BisqText.BaseLight(quoteAmountLabel)
            BisqGap.VHalf()
            AutoResizeText(
                text = priceText,
                textStyle = BisqTheme.typography.smallLight,
                maxLines = 1,
                modifier = Modifier.testTag("offerCard.price"),
            )
            if (agePlacement == AgePlacement.OWN_LINE) {
                AutoResizeText(
                    text = ageLabel,
                    textStyle = BisqTheme.typography.xsmallRegular,
                    color = BisqTheme.colors.mid_grey30,
                    maxLines = 1,
                    minimumFontSize = 10.sp,
                    modifier = Modifier.testTag("offerCard.age"),
                )
            }

            BisqGap.VHalf()
            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SimulatedPaymentMethods(quoteCount = quotePaymentCount, baseCount = basePaymentCount)
                if (agePlacement == AgePlacement.PAYMENTS_ROW) {
                    OfferAgeLabel(
                        text = ageLabel,
                        accessibilityLabel = ageAccessibilityLabel,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (isMyOffer) {
                    Box(
                        modifier =
                            Modifier
                                .padding(start = BisqUIConstants.ScreenPadding)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(BisqTheme.colors.danger.copy(alpha = 0.6f)),
                    )
                }
            }
            BisqGap.VHalf()
        }
    }
}

/**
 * The age, right aligned in whatever width the payment-methods row leaves free. Steps down from
 * 12sp to 10sp and is then omitted; never wraps, never ellipsizes, never changes the row height.
 */
@ExcludeFromCoverage
@Composable
internal fun OfferAgeLabel(
    text: String,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.padding(start = BisqUIConstants.ScreenPadding),
        contentAlignment = Alignment.CenterEnd,
    ) {
        // 0 = 12sp, 1 = 10sp, 2 = omitted. Re-derived whenever the text or the free width changes.
        var step by remember(text, maxWidth) { mutableIntStateOf(0) }
        if (step < 2) {
            BisqText.StyledText(
                text = text,
                style = if (step == 0) BisqTheme.typography.xsmallRegular else ageCompactStyle(),
                color = BisqTheme.colors.mid_grey30,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                onTextLayout = { layout -> if (layout.hasVisualOverflow) step++ },
                modifier =
                    Modifier
                        .semantics { contentDescription = accessibilityLabel }
                        .testTag("offerCard.age"),
            )
        }
    }
}

@Composable
private fun ageCompactStyle() = BisqTheme.typography.xsmallRegular.copy(fontSize = 10.sp)

/** Stand-in for the payment-method icons: 20dp squares, quote side, swap glyph gap, base side. */
@Composable
private fun SimulatedPaymentMethods(
    quoteCount: Int,
    baseCount: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(quoteCount) { SimulatedMethodIcon() }
        }
        Box(
            modifier =
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(BisqTheme.colors.mid_grey10),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(baseCount) { SimulatedMethodIcon() }
        }
    }
}

@Composable
private fun SimulatedMethodIcon() {
    Box(
        modifier =
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BisqTheme.colors.mid_grey20),
    )
}

// =============================================================================================
// Preview helpers
// =============================================================================================

@Composable
private fun PeerCard(
    price: String,
    fraction: Double?,
    ageDays: Long,
    modifier: Modifier = Modifier,
    isBuy: Boolean = true,
    amount: String = "500 USD",
    userName: String = "Satoshi",
    quotePaymentCount: Int = 2,
    basePaymentCount: Int = 1,
    agePlacement: AgePlacement = AgePlacement.PAYMENTS_ROW,
    contactTag: String? = null,
) {
    SimulatedOfferCard(
        directionLabel = if (isBuy) "Buy Bitcoin from" else "Sell Bitcoin to",
        isBuyDirection = isBuy,
        isMyOffer = false,
        userName = userName,
        quoteAmountLabel = amount,
        priceText = priceLine(price, fraction),
        ageLabel = simulatedOfferAgeLabel(ageDays),
        ageAccessibilityLabel = "Offer age: ${simulatedOfferAgeLabel(ageDays)}",
        quotePaymentCount = quotePaymentCount,
        basePaymentCount = basePaymentCount,
        agePlacement = agePlacement,
        contactTag = contactTag,
        modifier = modifier,
    )
}

@Composable
private fun OwnCard(
    price: String,
    fraction: Double?,
    ageDays: Long,
    modifier: Modifier = Modifier,
    quotePaymentCount: Int = 2,
    basePaymentCount: Int = 1,
) {
    SimulatedOfferCard(
        directionLabel = "Offer to sell BTC",
        isBuyDirection = false,
        isMyOffer = true,
        userName = "",
        quoteAmountLabel = "100 - 500 USD",
        priceText = priceLine(price, fraction),
        ageLabel = simulatedOfferAgeLabel(ageDays),
        ageAccessibilityLabel = "Offer age: ${simulatedOfferAgeLabel(ageDays)}",
        quotePaymentCount = quotePaymentCount,
        basePaymentCount = basePaymentCount,
        agePlacement = AgePlacement.PAYMENTS_ROW,
        modifier = modifier,
    )
}

@Composable
private fun PreviewColumn(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .background(BisqTheme.colors.backgroundColor)
                .padding(BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        content()
    }
}

// =============================================================================================
// Previews: price wording and age, peer offers
// =============================================================================================

@ExcludeFromCoverage
@Preview(name = "Card 1: today, before (no age, bare price) vs after", heightDp = 400)
@Composable
private fun Card_BeforeAfter_Preview() {
    BisqTheme.Preview {
        PreviewColumn {
            BisqText.SmallLightGrey("Before")
            SimulatedOfferCard(
                directionLabel = "Buy Bitcoin from",
                isBuyDirection = true,
                isMyOffer = false,
                userName = "Satoshi",
                quoteAmountLabel = "500 USD",
                priceText = "@ 51,000 (+2.00 %)",
                ageLabel = "",
                ageAccessibilityLabel = "",
                quotePaymentCount = 2,
                basePaymentCount = 1,
                agePlacement = AgePlacement.NONE,
            )
            BisqText.SmallLightGrey("After")
            PeerCard(price = "51,000", fraction = 0.02, ageDays = 0)
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Card 2: fixed price, today", heightDp = 180)
@Composable
private fun Card_FixedToday_Preview() {
    BisqTheme.Preview {
        PreviewColumn { PeerCard(price = "50,000", fraction = null, ageDays = 0) }
    }
}

@ExcludeFromCoverage
@Preview(name = "Card 3: floating +2.00 %, 1 day", heightDp = 180)
@Composable
private fun Card_FloatPlusOneDay_Preview() {
    BisqTheme.Preview {
        PreviewColumn { PeerCard(price = "51,000", fraction = 0.02, ageDays = 1) }
    }
}

@ExcludeFromCoverage
@Preview(name = "Card 4: floating -2.00 %, 12 days, sell direction", heightDp = 180)
@Composable
private fun Card_FloatMinus12Days_Preview() {
    BisqTheme.Preview {
        PreviewColumn { PeerCard(price = "49,000", fraction = -0.02, ageDays = 12, isBuy = false) }
    }
}

@ExcludeFromCoverage
@Preview(name = "Card 5: floating rounds to 0.00 %, Market price", heightDp = 180)
@Composable
private fun Card_FloatZero_Preview() {
    BisqTheme.Preview {
        PreviewColumn { PeerCard(price = "50,000", fraction = -0.00004, ageDays = 3) }
    }
}

@ExcludeFromCoverage
@Preview(name = "Card 6: long price and wide range, 3-digit age", heightDp = 180)
@Composable
private fun Card_LongPriceRange_Preview() {
    BisqTheme.Preview {
        PreviewColumn {
            PeerCard(
                price = "92,662,350.35",
                fraction = 0.1877,
                ageDays = 112,
                amount = "100,000 - 1,000,000 JPY",
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Card 7: crowded payment row (4 + 2 icons), age steps down", heightDp = 180)
@Composable
private fun Card_CrowdedPaymentRow_Preview() {
    BisqTheme.Preview {
        PreviewColumn {
            PeerCard(
                price = "50,000",
                fraction = null,
                ageDays = 112,
                quotePaymentCount = 4,
                basePaymentCount = 2,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Card 8: full payment row (5 + 3 icons), age omitted", heightDp = 180)
@Composable
private fun Card_FullPaymentRow_Preview() {
    BisqTheme.Preview {
        PreviewColumn {
            PeerCard(
                price = "50,000",
                fraction = null,
                ageDays = 112,
                quotePaymentCount = 5,
                basePaymentCount = 3,
            )
        }
    }
}

// =============================================================================================
// Previews: own offers, contacts, lists
// =============================================================================================

@ExcludeFromCoverage
@Preview(name = "Own 1: fixed, today (age left of remove icon)", heightDp = 180)
@Composable
private fun Own_FixedToday_Preview() {
    BisqTheme.Preview {
        PreviewColumn { OwnCard(price = "50,000", fraction = null, ageDays = 0) }
    }
}

@ExcludeFromCoverage
@Preview(name = "Own 2: floating +, crowded row, 14 days", heightDp = 180)
@Composable
private fun Own_FloatCrowded_Preview() {
    BisqTheme.Preview {
        PreviewColumn {
            OwnCard(
                price = "51,000",
                fraction = 0.02,
                ageDays = 14,
                quotePaymentCount = 4,
                basePaymentCount = 2,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Contact 1: tagged contact card (168dp) with age", heightDp = 210)
@Composable
private fun Contact_Tagged_Preview() {
    BisqTheme.Preview {
        PreviewColumn {
            PeerCard(
                price = "51,000",
                fraction = 0.02,
                ageDays = 5,
                contactTag = "Reliable SEPA trader",
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "List 1: mixed list, amounts stay at the same y", heightDp = 860)
@Composable
private fun List_Mixed_Preview() {
    BisqTheme.Preview {
        PreviewColumn {
            PeerCard(price = "50,000", fraction = null, ageDays = 0)
            OwnCard(price = "51,000", fraction = 0.02, ageDays = 3)
            PeerCard(price = "49,000", fraction = -0.02, ageDays = 1, isBuy = false)
            PeerCard(
                price = "51,000",
                fraction = 0.02,
                ageDays = 27,
                contactTag = "Fast payer",
            )
            PeerCard(price = "50,000", fraction = null, ageDays = 365)
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Alt: age on its own line (+14dp) vs recommended (+0dp)", heightDp = 420)
@Composable
private fun Card_AgePlacementComparison_Preview() {
    BisqTheme.Preview {
        PreviewColumn {
            BisqText.SmallLightGrey("Recommended: payment-methods row, card stays 150dp")
            PeerCard(price = "51,000", fraction = 0.02, ageDays = 12)
            BisqText.SmallLightGrey("Alternative: own line, card grows to about 164dp")
            PeerCard(
                price = "51,000",
                fraction = 0.02,
                ageDays = 12,
                agePlacement = AgePlacement.OWN_LINE,
            )
        }
    }
}

// =============================================================================================
// Previews: stress
// =============================================================================================

@ExcludeFromCoverage
@Preview(name = "Stress 1: long translation (German)", heightDp = 180)
@Composable
private fun Stress_LongTranslation_Preview() {
    BisqTheme.Preview {
        PreviewColumn {
            SimulatedOfferCard(
                directionLabel = "Bitcoin kaufen von",
                isBuyDirection = true,
                isMyOffer = false,
                userName = "Satoshi",
                quoteAmountLabel = "500 USD",
                priceText = "@ 51.000 (+2,00 % ueber dem Marktpreis)",
                ageLabel = "vor 112 Tagen",
                ageAccessibilityLabel = "Alter des Angebots: vor 112 Tagen",
                quotePaymentCount = 3,
                basePaymentCount = 2,
                agePlacement = AgePlacement.PAYMENTS_ROW,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Stress 2: large font (fontScale 1.5)", heightDp = 200, fontScale = 1.5f)
@Composable
private fun Stress_LargeFont_Preview() {
    BisqTheme.Preview {
        PreviewColumn { PeerCard(price = "51,000", fraction = 0.02, ageDays = 12) }
    }
}

@ExcludeFromCoverage
@Preview(name = "Stress 3: narrow screen (320dp)", widthDp = 320, heightDp = 360)
@Composable
private fun Stress_NarrowScreen_Preview() {
    BisqTheme.Preview {
        PreviewColumn {
            PeerCard(price = "51,000", fraction = 0.02, ageDays = 112, quotePaymentCount = 3)
            OwnCard(price = "51,000", fraction = 0.02, ageDays = 112, quotePaymentCount = 3)
        }
    }
}

// =============================================================================================
// Previews: take-offer review price wording
// =============================================================================================

@ExcludeFromCoverage
@Preview(name = "Review price 1: fixed", heightDp = 800)
@Composable
private fun ReviewPrice_Fixed_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = null,
            priceTypeLabel = simulatedPriceTypeLabel(null),
            priceDetail = SIMULATED_PRICE_DETAIL_FIXED,
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review price 2: floating +", heightDp = 800)
@Composable
private fun ReviewPrice_FloatPlus_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = null,
            priceTypeLabel = simulatedPriceTypeLabel(0.02),
            priceDetail = SIMULATED_PRICE_DETAIL_FLOAT_ABOVE,
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review price 3: floating -", heightDp = 800)
@Composable
private fun ReviewPrice_FloatMinus_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = null,
            priceTypeLabel = simulatedPriceTypeLabel(-0.0150),
            priceDetail = SIMULATED_PRICE_DETAIL_FLOAT_BELOW,
            price = "44,325",
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review price 4: market price", heightDp = 800)
@Composable
private fun ReviewPrice_Market_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = null,
            priceTypeLabel = simulatedPriceTypeLabel(0.0),
            priceDetail = SIMULATED_PRICE_DETAIL_MARKET,
            price = "45,000",
        )
    }
}
