package network.bisq.mobile.presentation.design.contacts_offerbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/**
 * Design PoC — "this offer maker is one of my contacts" indicator on the offerbook offer card.
 *
 * Branch `feature/my_contacts_connect_wiring`, Milestone 11 "Bisq Community". Evaluates and
 * extends rodvar's initial idea (tag under the stars, generic badge fallback) against the
 * production [network.bisq.mobile.presentation.offerbook.OfferCard] and the existing directory
 * row [network.bisq.mobile.presentation.community.contacts.ContactCard].
 *
 * DO NOT wire this into production. This file exists to be opened in Android Studio's Split /
 * Design view and reviewed via its `@Preview` functions.
 *
 * ---------------------------------------------------------------------------------------------
 * ## 1. Is this worth doing?
 *
 * Yes. In a P2P exchange with no central counterparty vetting, the buyer/seller relationship
 * *is* the trust boundary — a note like "Reliable SEPA trader, met at conference" surfaced right
 * where the take-offer decision happens is exactly the kind of information that changes user
 * behavior (which of ten similar-looking SEPA offers to take). Withholding it behind a profile
 * tap defeats the purpose: fast offerbook scanning is the whole interaction model, and a signal
 * that only appears after committing to open a peer's profile arrives too late to help pick an
 * offer. The cost is real (more visual load on an already dense 150dp card, in a list the user
 * scans quickly) but it is bounded and only paid on the minority of cards that are contacts —
 * see §5.
 *
 * ## 2. Placement — recommendation and rejected options
 *
 * **Recommended: PRIMARY — a compact pill under the languages row** (rodvar's instinct,
 * refined). See [ContactIndicatorVariant.UNDER_LANGUAGES]. It sits in the identity column, next
 * to the other maker-authored metadata (rating, languages), reads it as one more fact about the
 * maker rather than a claim about offer quality, and — critically — carries the tag text itself,
 * which is the highest-value part of the signal per §1.
 *
 * **Shown as an alternative for comparison: avatar corner badge**, see
 * [ContactIndicatorVariant.AVATAR_BADGE]. Pros: zero vertical growth, sits on the identity anchor
 * users already recognize. Cons — why it is NOT the primary recommendation:
 * - It can only communicate presence, not the tag text. The tag is the valuable part (§1); a
 *   badge alone would still force a profile tap to read a note the user wrote for exactly this
 *   moment, which undermines the entire point of surfacing it.
 * - At the ~46-54dp avatar size on this card, a legible badge is a very small hit target for the
 *   eye during fast scanning — easy to miss, unlike a pill with a full word or two of text.
 * - It visually echoes "verified" badges (checkmarks on avatars) common in other apps, which are
 *   platform/authority-issued trust marks. This indicator is the *user's own* private
 *   designation, and biasing it toward that established "verified" visual language is exactly
 *   the misread §4 warns against.
 *
 * **Rejected outright:**
 * - *Chip next to the username* (in the "Buy Bitcoin from [username]" line): that line already
 *   carries the direction-color semantic (buy=green/sell=red) and is the primary scan target for
 *   "what is this offer." Layering a third piece of information into one already-busy row hurts
 *   the thing the row exists for. It also conflicts with the direction color itself (§4).
 * - *Card-level accent* (tinted background/border for contact offers): too strong — reads as a
 *   quality/ranking signal across the whole card, which is precisely the "safer offer"
 *   endorsement risk in §4. The offerbook already uses background tint for `isMyOffer` and
 *   `isInvalidDueToReputation`; adding a third background-tint meaning on the same card starts
 *   to overload the one visual channel a user relies on to triage the whole list at a glance.
 *
 * ## 3. Tag vs. generic label, truncation
 *
 * - Tag present (≤30 chars per [network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO]):
 *   shown verbatim as the pill's only content — no "Tag:" prefix, matching the existing
 *   [network.bisq.mobile.presentation.community.contacts.ContactCard]'s `ContactTagPill`
 *   convention of letting the tag speak for itself.
 * - Tag absent: falls back to a short generic label ("Contact") in the same pill — presence
 *   itself is still worth signalling even without a note.
 * - Truncation: the identity column is only ~weight(1) of the row (roughly 100-120dp at typical
 *   phone widths, per the reference screenshot) — far narrower than `ContactCard`'s full-row
 *   pill. `AutoResizeText(maxLines = 1, minimumFontSize = 8.sp, overflow = Ellipsis)` shrinks
 *   before it truncates, matching the shrink-then-ellipsis pattern already used for the
 *   languages row and username chip in `OfferCard`/`UserProfile`. A 30-char tag WILL usually
 *   truncate at this width — that's an accepted trade-off: this card's job is a recognition cue
 *   ("I know this person, and roughly why"), not the full note, which remains available in full
 *   on the Contacts directory / peer profile. See [ContactOfferCard_LongTagPreview].
 *
 * ## 4. Color and iconography
 *
 * Green (`primary`) is already `isBuy` + `isMyOffer` background; red (`danger`) is `isSell`;
 * orange (`warning`) and amber (`yellow`) are reserved for warnings / in-mediation states
 * elsewhere in the app (see agent memory on the trade-status-out-of-sync work). Reusing any of
 * them here would make the contact indicator look like a Bisq-computed trust/quality signal
 * riding on an offer, when it is actually the *viewing user's own, private, unverified*
 * designation of someone they chose to remember. That distinction matters in a decentralized
 * app: Bisq must never look like it is vouching for a trade.
 *
 * This POC deliberately uses a **neutral grey pill** — `dark_grey50` background /
 * `light_grey20` text — the same visual family as `ContactCard`'s own `ContactReasonPill`
 * (metadata, not endorsement), and NOT the green `ContactTagPill` also in that same file. That
 * divergence from `ContactTagPill` is intentional, not an oversight: on the Contacts directory
 * every row is already a contact, so green there just means "this pill has content." On the
 * offerbook, green already means something transactional (buy / my-offer); reusing it for
 * "contact" would give green a third, conflicting meaning on the one screen where its meaning
 * needs to stay unambiguous for fast scanning.
 *
 * No icon glyph is used on the primary pill (text only, like `ContactReasonPill`) — a checkmark
 * or similar would read as "verified," the exact misread this section is designed to avoid. The
 * one icon in this file (a small `Icons.Filled.Check` glyph) appears only in the *alternative*
 * avatar-badge variant, where a caption-length text label has no room; even there it is rendered
 * in neutral grey/white, never green.
 *
 * **trustScore: OUT of v1**, per rodvar's framing question. `trustScore` is itself a private,
 * subjective 0..1 number the user assigns — showing it (as a percentage, a second star row, or
 * modulating pill color/prominence by it) right next to the actual, network-computed reputation
 * stars invites confusing the two. It also adds a second numeric trust signal at exactly the
 * moment a user is trying to read ONE reputation signal (the stars) at a glance. Recommendation:
 * ship presence + tag only; revisit trustScore-driven emphasis later if data shows users want it,
 * and if so put it on the Contacts directory / peer profile first, not the offerbook.
 *
 * ## 5. Vertical space
 *
 * The identity column already stacks avatar + stars + languages inside a fixed 150dp card. There
 * is no slack left to add a fourth stacked element for free. Two options were considered:
 * - **Always-reserve a slot** (the pattern `ContactCard` uses for uniform directory-row height):
 *   rejected here — in a directory every row is a contact, so paying a fixed height cost on every
 *   row is "free" (no waste). In the offerbook, contacts are the minority of offers; reserving an
 *   invisible slot on every card would waste space on most of the list for a feature most cards
 *   don't have.
 * - **Conditional height growth** (adopted): only cards where `isContact` is true and the primary
 *   variant is active grow from 150dp to 150dp + `BisqUIConstants.ScreenPadding2X` (174dp) to fit
 *   the pill. Non-contact cards are pixel-identical to today. The trade-off is uneven row heights
 *   while scrolling a mixed list — accepted because it only affects the subset of cards carrying
 *   genuinely new, high-value information, and because a LazyColumn already handles variable item
 *   heights natively; there is no measurable performance cost.
 * - The avatar-badge alternative needs no height change at all, which is its one clear structural
 *   advantage — noted for completeness even though it is not the primary recommendation.
 *
 * ## i18n
 *
 * All strings are hardcoded English in this PoC (per project convention: new keys land in
 * `mobile.properties` English-only; translations are propagated separately). Keys a real
 * implementation would introduce:
 * - `mobile.bisqEasy.offerbook.offerCard.contact.genericLabel` = "Contact" — shown when the
 *   maker is a contact with no tag set.
 * - Optionally `mobile.bisqEasy.offerbook.offerCard.contact.a11yLabel` = "{0} is one of your
 *   contacts" — a screen-reader description for the pill/badge; not wired in this PoC (see
 *   [ExcludeFromCoverage] usage below — production accessibility wiring is deferred to
 *   implementation, consistent with this PoC's contentDescription being left `null`).
 *
 * The maker's own `tag` is user-authored free text and is never translated — it renders verbatim
 * regardless of locale, same as `ContactCard`.
 */
private const val GENERIC_CONTACT_LABEL = "Contact"

/** Which placement treatment a preview renders. See design rationale §2 above. */
internal enum class ContactIndicatorVariant {
    /** Recommended: pill under the languages row, carries the tag or the generic label. */
    UNDER_LANGUAGES,

    /** Comparison alternative: small badge on the avatar's corner, presence-only. */
    AVATAR_BADGE,
}

/**
 * Self-contained restage of the offerbook offer card (see production
 * [network.bisq.mobile.presentation.offerbook.OfferCard]) built from primitives only, with the
 * contact indicator integrated per the variant chosen. Not wired to any domain type, presenter,
 * or Koin — safe to render in `@Preview`.
 */
@ExcludeFromCoverage
@Composable
internal fun ContactOfferCard(
    directionLabel: String,
    isBuyDirection: Boolean,
    userName: String,
    starRating: Double,
    languageCodesLabel: String,
    quoteAmountLabel: String,
    priceLabel: String,
    paymentIconCount: Int,
    isContact: Boolean,
    contactTag: String?,
    variant: ContactIndicatorVariant,
    modifier: Modifier = Modifier,
) {
    val directionColor: Color =
        if (isBuyDirection) {
            BisqTheme.colors.primary.copy(alpha = 0.8f)
        } else {
            BisqTheme.colors.danger.copy(alpha = 0.8f)
        }

    val showInlineContactLine = isContact && variant == ContactIndicatorVariant.UNDER_LANGUAGES
    val showAvatarBadge = isContact && variant == ContactIndicatorVariant.AVATAR_BADGE
    val baseHeight = 150.dp
    // One compact pill row (§5) — only the contact-card-with-primary-variant case grows.
    val cardHeight = if (showInlineContactLine) baseHeight + BisqUIConstants.ScreenPadding2X else baseHeight

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(8.dp))
                .background(color = BisqTheme.colors.dark_grey50.copy(alpha = 0.9f))
                .height(cardHeight)
                .padding(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1.0F),
        ) {
            SimulatedAvatarPlaceholder(showContactBadge = showAvatarBadge)

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

            if (showInlineContactLine) {
                BisqGap.VHalf()
                ContactIndicatorPill(
                    text = contactTag?.takeIf { it.isNotBlank() } ?: GENERIC_CONTACT_LABEL,
                )
            }
        }

        BisqGap.H1()
        BisqVDivider(thickness = BisqUIConstants.ScreenPaddingQuarter, color = BisqTheme.colors.primary)
        BisqGap.H1()

        Column(
            modifier = Modifier.weight(3.0F).fillMaxHeight(),
        ) {
            Row(
                modifier = Modifier.height(40.dp),
            ) {
                BisqText.BaseRegular(
                    text = directionLabel,
                    color = directionColor,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )

                BisqGap.HHalf()

                AutoResizeText(
                    text = userName,
                    color = directionColor,
                    overflow = TextOverflow.Ellipsis,
                    textStyle = BisqTheme.typography.smallRegular,
                    maxLines = 2,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                            .background(BisqTheme.colors.dark_grey10.copy(alpha = 0.4f))
                            .padding(horizontal = BisqUIConstants.ScreenPaddingHalf, vertical = 2.dp)
                            .align(Alignment.CenterVertically),
                    minimumFontSize = 10.sp,
                )
            }

            BisqGap.VHalf()
            BisqText.BaseLight(quoteAmountLabel)
            BisqGap.VHalf()

            AutoResizeText(
                text = "@ $priceLabel",
                textStyle = BisqTheme.typography.smallLight,
                maxLines = 1,
            )

            BisqGap.VHalf()
            Spacer(modifier = Modifier.weight(1f))

            SimulatedPaymentIconsRow(count = paymentIconCount)
            BisqGap.VHalf()
        }
    }
}

/**
 * Neutral metadata pill — deliberately NOT the green [BisqTheme.colors.primary] used by
 * `ContactCard`'s `ContactTagPill`. See design rationale §4 for why the offerbook needs a
 * different, non-transactional color for this signal.
 */
@Composable
private fun ContactIndicatorPill(text: String) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                .background(BisqTheme.colors.dark_grey50)
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
 * Stand-in for the maker's avatar (production uses `UserProfileIcon`, which needs
 * `PlatformImage` + a suspend provider — out of scope for a primitive-only PoC). When
 * [showContactBadge] is true, overlays the avatar-corner-badge alternative from §2 — a small
 * neutral dot with a checkmark glyph, NOT green, to avoid reading as "verified."
 */
@Composable
private fun SimulatedAvatarPlaceholder(showContactBadge: Boolean) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier =
                Modifier
                    .size(BisqUIConstants.ScreenPadding4X)
                    .clip(CircleShape)
                    .background(BisqTheme.colors.mid_grey10),
        )
        if (showContactBadge) {
            Box(
                modifier =
                    Modifier
                        .size(BisqUIConstants.ScreenPadding2)
                        .clip(CircleShape)
                        .background(BisqTheme.colors.dark_grey20)
                        .padding(BisqUIConstants.ScreenPadding1),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(BisqUIConstants.ScreenPaddingHalfQuarter)
                            .clip(CircleShape)
                            .background(BisqTheme.colors.light_grey20),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = BisqTheme.colors.dark_grey20,
                        modifier = Modifier.size(BisqUIConstants.ScreenPaddingHalf),
                    )
                }
            }
        }
    }
}

/** Stand-in for the payment-method icon row — plain colored squares, no real icon assets. */
@Composable
private fun SimulatedPaymentIconsRow(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
        repeat(count) {
            Box(
                modifier =
                    Modifier
                        .size(BisqUIConstants.ScreenPadding)
                        .clip(RoundedCornerShape(BisqUIConstants.ScreenPadding1))
                        .background(BisqTheme.colors.mid_grey10),
            )
        }
    }
}

// ============================================================================================
// Previews
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "Contact, tagged — primary placement")
@Composable
private fun ContactOfferCard_TaggedPreview() {
    BisqTheme.Preview {
        ContactOfferCard(
            directionLabel = "Buy Bitcoin from",
            isBuyDirection = true,
            userName = "strayorigin",
            starRating = 5.0,
            languageCodesLabel = "EN",
            quoteAmountLabel = "100.00 - 249.00 USD",
            priceLabel = "92,662.35 BTC/USD (+18.77 %)",
            paymentIconCount = 4,
            isContact = true,
            contactTag = "Reliable SEPA trader",
            variant = ContactIndicatorVariant.UNDER_LANGUAGES,
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Contact, no tag — generic badge fallback")
@Composable
private fun ContactOfferCard_NoTagPreview() {
    BisqTheme.Preview {
        ContactOfferCard(
            directionLabel = "Buy Bitcoin from",
            isBuyDirection = true,
            userName = "NewTrader0007",
            starRating = 3.5,
            languageCodesLabel = "EN, DE",
            quoteAmountLabel = "50.00 - 150.00 EUR",
            priceLabel = "85,120.00 BTC/EUR (+2.10 %)",
            paymentIconCount = 3,
            isContact = true,
            contactTag = null,
            variant = ContactIndicatorVariant.UNDER_LANGUAGES,
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Contact, 30-char (max) tag — truncation check")
@Composable
private fun ContactOfferCard_LongTagPreview() {
    BisqTheme.Preview {
        ContactOfferCard(
            directionLabel = "Buy Bitcoin from",
            isBuyDirection = true,
            userName = "BitcoinTrader",
            starRating = 4.5,
            languageCodesLabel = "EN",
            quoteAmountLabel = "200.00 - 500.00 USD",
            priceLabel = "91,004.10 BTC/USD (+9.40 %)",
            paymentIconCount = 5,
            isContact = true,
            contactTag = "Met at conf, trades big SEPA amt", // 33 chars — over the 30-char cap on purpose, worst case
            variant = ContactIndicatorVariant.UNDER_LANGUAGES,
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Not a contact — unchanged baseline")
@Composable
private fun ContactOfferCard_NotContactPreview() {
    BisqTheme.Preview {
        ContactOfferCard(
            directionLabel = "Buy Bitcoin from",
            isBuyDirection = true,
            userName = "SatoshiNakamoto",
            starRating = 5.0,
            languageCodesLabel = "EN",
            quoteAmountLabel = "500.00 EUR",
            priceLabel = "50,000",
            paymentIconCount = 4,
            isContact = false,
            contactTag = null,
            variant = ContactIndicatorVariant.UNDER_LANGUAGES,
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Contact, sell direction, tagged")
@Composable
private fun ContactOfferCard_SellDirectionPreview() {
    BisqTheme.Preview {
        ContactOfferCard(
            directionLabel = "Sell Bitcoin to",
            isBuyDirection = false,
            userName = "BitcoinTrader",
            starRating = 4.0,
            languageCodesLabel = "EN, FR",
            quoteAmountLabel = "1,000.00 EUR",
            priceLabel = "52,000",
            paymentIconCount = 3,
            isContact = true,
            contactTag = "Fast payer",
            variant = ContactIndicatorVariant.UNDER_LANGUAGES,
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "ALTERNATIVE — avatar corner badge, tagged contact")
@Composable
private fun ContactOfferCard_AvatarBadgeVariantPreview() {
    BisqTheme.Preview {
        ContactOfferCard(
            directionLabel = "Buy Bitcoin from",
            isBuyDirection = true,
            userName = "strayorigin",
            starRating = 5.0,
            languageCodesLabel = "EN",
            quoteAmountLabel = "100.00 - 249.00 USD",
            priceLabel = "92,662.35 BTC/USD (+18.77 %)",
            paymentIconCount = 4,
            isContact = true,
            contactTag = "Reliable SEPA trader", // present but unused by this variant — see §2
            variant = ContactIndicatorVariant.AVATAR_BADGE,
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Side-by-side — contact vs. non-contact in a list")
@Composable
private fun ContactOfferCard_ListComparisonPreview() {
    BisqTheme.Preview {
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
            ContactOfferCard(
                directionLabel = "Buy Bitcoin from",
                isBuyDirection = true,
                userName = "strayorigin",
                starRating = 5.0,
                languageCodesLabel = "EN",
                quoteAmountLabel = "100.00 - 249.00 USD",
                priceLabel = "92,662.35 BTC/USD (+18.77 %)",
                paymentIconCount = 4,
                isContact = true,
                contactTag = "Reliable SEPA trader",
                variant = ContactIndicatorVariant.UNDER_LANGUAGES,
            )
            ContactOfferCard(
                directionLabel = "Sell Bitcoin to",
                isBuyDirection = false,
                userName = "RandomTrader88",
                starRating = 2.5,
                languageCodesLabel = "EN",
                quoteAmountLabel = "300.00 EUR",
                priceLabel = "51,500",
                paymentIconCount = 3,
                isContact = false,
                contactTag = null,
                variant = ContactIndicatorVariant.UNDER_LANGUAGES,
            )
            ContactOfferCard(
                directionLabel = "Buy Bitcoin from",
                isBuyDirection = true,
                userName = "AnotherContact",
                starRating = 4.0,
                languageCodesLabel = "EN, DE",
                quoteAmountLabel = "150.00 - 300.00 EUR",
                priceLabel = "89,900.00 BTC/EUR (+1.20 %)",
                paymentIconCount = 4,
                isContact = true,
                contactTag = null,
                variant = ContactIndicatorVariant.UNDER_LANGUAGES,
            )
        }
    }
}
