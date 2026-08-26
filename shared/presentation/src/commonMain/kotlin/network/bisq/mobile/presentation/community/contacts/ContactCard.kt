package network.bisq.mobile.presentation.community.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import kotlin.math.roundToInt

/**
 * One row of the Contacts directory: avatar on the LEFT (per the IA decision — this is a
 * directory, not an inbox, so the avatar/name pairing reads like a contact book entry, not
 * a chat participant), with nickname, the user's own tag (if any), why this peer became a
 * contact, a compact trust indicator, and the date added. Deliberately does not surface
 * [ContactListItemUiState.tag]'s longer sibling `notes` (600-char free text) — that belongs
 * on the Peer Profile screen, not a directory row that must stay scannable.
 *
 * The whole card is the tap target (`onClick`) — Peer Profile navigation is wired by the
 * caller; this composable only exposes the callback.
 */
@Composable
fun ContactCard(
    contact: ContactListItemUiState,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BisqCard(
        modifier =
            modifier
                .fillMaxWidth()
                .debouncedClickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            UserProfileIcon(
                userProfile = contact.peerProfile,
                userProfileIconProvider = userProfileIconProvider,
                size = BisqUIConstants.ScreenPadding4X,
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BisqText.BaseRegular(
                        text = contact.peerProfile.userName,
                        color = BisqTheme.colors.white,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    BisqText.SmallRegularGrey(text = contact.dateAddedLabel)
                }

                BisqGap.VQuarter()
                // The tag slot is ALWAYS reserved: an invisible placeholder pill keeps untagged
                // cards the exact same height as tagged ones (uniform directory rhythm), while
                // still growing with the user's font scale — unlike a hard-coded card height,
                // which would clip at accessibility font sizes.
                if (contact.tag != null) {
                    ContactTagPill(tag = contact.tag)
                } else {
                    ContactTagPill(tag = " ", modifier = Modifier.alpha(0f))
                }

                BisqGap.VHalf()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // weight(fill = false) caps the pill at "whatever's left after the trust
                    // indicator's fixed footprint below" — it can never crush that sibling to
                    // a sliver, matching or exceeding any reason/MISSING-placeholder length.
                    ContactReasonPill(
                        reason = contact.contactReason,
                        modifier = Modifier.weight(1f, fill = false).padding(end = BisqUIConstants.ScreenPaddingHalf),
                    )
                    ContactTrustScoreIndicator(trustScore = contact.trustScore)
                }
            }
        }
    }
}

/**
 * Renders the user's own tag on the contact, single-line: the text auto-resizes down before
 * anything is lost (tags cap at 30 chars per the bisq2 core contract), keeping every card the
 * same height. `width(IntrinsicSize.Max)` makes the pill hug its single-line content width
 * (bounded by the incoming constraints) instead of stretching to the full column — and unlike
 * `IntrinsicSize.Min` it does not force a wrap at every word boundary.
 */
@Composable
internal fun ContactTagPill(
    tag: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .width(IntrinsicSize.Max)
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                .background(BisqTheme.colors.primary.copy(alpha = 0.15f))
                .padding(horizontal = BisqUIConstants.ScreenPaddingHalf, vertical = BisqUIConstants.ScreenPaddingQuarter),
    ) {
        AutoResizeText(
            text = tag,
            textStyle = BisqTheme.typography.xsmallMedium,
            color = BisqTheme.colors.primary,
            maxLines = 1,
        )
    }
}

/**
 * Same intrinsic-width hugging as [ContactTagPill] (see its KDoc), plus a caller-supplied
 * `modifier` that carries `weight(1f, fill = false)` from [ContactCard] — that weight caps
 * this pill's maximum width to whatever the row has left over after the trust indicator's
 * fixed footprint, so a long (or, pre-i18n-sync, `MISSING: [...]`-placeholder-length) reason
 * label can never crush [ContactTrustScoreIndicator] down to a sliver.
 */
@Composable
private fun ContactReasonPill(
    reason: ContactReasonEnum,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .width(IntrinsicSize.Max)
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                .background(BisqTheme.colors.dark_grey50)
                .padding(horizontal = BisqUIConstants.ScreenPaddingHalf, vertical = BisqUIConstants.ScreenPaddingQuarter),
    ) {
        AutoResizeText(
            text = reason.label(),
            textStyle = BisqTheme.typography.xsmallRegular,
            color = BisqTheme.colors.mid_grey20,
            maxLines = 1,
        )
    }
}

/**
 * Compact trust indicator: a short progress bar + percentage. Uses [BisqProgressBar]
 * rather than [network.bisq.mobile.presentation.common.ui.components.atoms.StarRating] on
 * purpose — the 5-star widget is reserved for [network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO]
 * (offer/trade reputation) elsewhere in the app; reusing it here would visually conflate
 * two different signals.
 *
 * The bar has a FIXED width and the percentage takes its natural single-line width — no
 * weight/fixed-box interplay, so no measure order can ever squeeze the text into a
 * vertical stack of characters, regardless of how much width the sibling
 * [ContactReasonPill] claims.
 */
@Composable
internal fun ContactTrustScoreIndicator(trustScore: Double) {
    val clamped = trustScore.coerceIn(0.0, 1.0)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
    ) {
        BisqProgressBar(
            progress = clamped.toFloat(),
            modifier =
                Modifier
                    .width(BisqUIConstants.ScreenPadding4X)
                    .height(BisqUIConstants.ScreenPadding2),
        )
        BisqText.StyledText(
            text = "${(clamped * 100).roundToInt()}%",
            style = BisqTheme.typography.xsmallRegular,
            color = BisqTheme.colors.mid_grey20,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun ContactReasonEnum.label(): String =
    when (this) {
        ContactReasonEnum.PRIVATE_CHAT -> "mobile.community.contacts.reason.privateChat".i18n()
        ContactReasonEnum.BISQ_EASY_TRADE -> "mobile.community.contacts.reason.bisqEasyTrade".i18n()
        ContactReasonEnum.MUSIG_TRADE -> "mobile.community.contacts.reason.musigTrade".i18n()
        ContactReasonEnum.MANUALLY_ADDED -> "mobile.community.contacts.reason.manuallyAdded".i18n()
    }

// ============================================================================================
// Preview fixtures
// ============================================================================================

private val previewUserProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

internal fun sampleContact(
    id: String,
    peerName: String,
    trustScore: Double,
    contactReason: ContactReasonEnum,
    dateAddedLabel: String,
    tag: String? = null,
): ContactListItemUiState =
    ContactListItemUiState(
        id = id,
        peerProfile = createMockUserProfile(peerName),
        trustScore = trustScore,
        contactReason = contactReason,
        dateAddedLabel = dateAddedLabel,
        tag = tag,
    )

// ============================================================================================
// Previews
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "Contact card — tagged, high trust, manually added")
@Composable
private fun ContactCard_TaggedHighTrustPreview() {
    BisqTheme.Preview {
        ContactCard(
            contact =
                sampleContact(
                    id = "contact-1",
                    peerName = "SatoshiFan#1234",
                    trustScore = 0.92,
                    contactReason = ContactReasonEnum.MANUALLY_ADDED,
                    dateAddedLabel = "12 Jul 2026",
                    tag = "Reliable SEPA trader",
                ),
            userProfileIconProvider = previewUserProfileIconProvider,
            onClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Contact card — no tag, low trust, from private chat")
@Composable
private fun ContactCard_NoTagLowTrustPreview() {
    BisqTheme.Preview {
        ContactCard(
            contact =
                sampleContact(
                    id = "contact-2",
                    peerName = "NewTrader#0007",
                    trustScore = 0.1,
                    contactReason = ContactReasonEnum.PRIVATE_CHAT,
                    dateAddedLabel = "2 days ago",
                ),
            userProfileIconProvider = previewUserProfileIconProvider,
            onClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Contact card — long tag wraps")
@Composable
private fun ContactCard_LongTagPreview() {
    BisqTheme.Preview {
        ContactCard(
            contact =
                sampleContact(
                    id = "contact-3",
                    peerName = "BitcoinTrader",
                    trustScore = 0.55,
                    contactReason = ContactReasonEnum.MUSIG_TRADE,
                    dateAddedLabel = "3 weeks ago",
                    tag = "Met at conference, trades large SEPA amounts reliably",
                ),
            userProfileIconProvider = previewUserProfileIconProvider,
            onClick = {},
        )
    }
}
