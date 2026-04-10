/**
 * # OpenPrivateChatButton
 *
 * The entry-point component for starting or resuming a private chat with any peer.
 *
 * This component is designed to be **placed inline wherever a user's profile avatar
 * appears** in the app — offerbook offer rows, trade peer headers, reputation cards,
 * user profile detail sheets, etc. Tapping it navigates directly to the private chat
 * thread with that peer.
 *
 * ## Desktop reference
 * On the desktop, "private message" is initiated via a context menu that appears
 * when hovering over a user profile. There is no mobile analogue for hover.
 * Mobile pattern: a small icon button adjacent to the avatar.
 *
 * We do NOT use a long-press on the avatar itself because:
 * 1. The avatar already handles long-press in some contexts (e.g., copy profile ID).
 * 2. Long-press is not discoverable — many users never find it.
 * 3. Icon buttons are always visible and affordant.
 *
 * ## Placement in the app
 *
 * ### 1. Offerbook offer rows
 * The peer avatar is shown in each offer card. The chat button sits to the right
 * of the avatar, replacing or supplementing the existing action. Implementation
 * note: check the OfferItemView for where to inject this.
 *
 * ### 2. Trade peer header (OpenTradeScreen)
 * The trade partner's profile row already uses UserProfileRow. The button can
 * be appended as a trailing action on that row.
 *
 * ### 3. User Profile detail sheet / bottom sheet
 * When tapping any user avatar to see full profile details, "Send private message"
 * is a primary CTA action button at the bottom of the sheet.
 *
 * ### 4. Reputation / user detail screens
 * Same pattern — a prominent button.
 *
 * ## Two visual variants
 *
 * The component has two visual modes controlled by [variant]:
 *
 * **ICON_BUTTON** (default for inline placement)
 * A small circular icon button (32 dp) showing a chat bubble icon.
 * Fits next to avatars without disrupting the row layout.
 * This is the right choice for offerbook rows and trade peer headers.
 *
 * **TEXT_BUTTON** (for prominent placement in sheets / detail screens)
 * A full-width BisqButton-style button reading "Send private message".
 * This is the right choice for user profile bottom sheets and detail screens
 * where there is more space to communicate the intent clearly to first-time users.
 *
 * ## Loading state
 * Opening a chat requires a round-trip to create/find the channel on the backend.
 * During this window (typically < 200 ms on clearnet, up to 2–3 s over Tor):
 * - ICON_BUTTON: icon is replaced with a small CircularProgressIndicator.
 * - TEXT_BUTTON: button shows standard BisqButton loading state (spinner in text).
 *
 * ## Privacy consideration
 * The button should only be shown for profiles that are NOT the local user's own
 * profile. The presenter must enforce this by not rendering the button for own profiles.
 *
 * ## i18n
 * mobile.privateChats.openChat = "Send private message"
 * mobile.privateChats.openChat.loading = "Opening..."
 *
 * ## Accessibility
 * Content description for icon variant: "mobile.privateChats.openChat".i18n() + " " + peerName
 * This gives screen readers: "Send private message to SatoshiFan#1234"
 */
package network.bisq.mobile.presentation.design.privatechat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

// ---------------------------------------------------------------------------
// Variant enum
// ---------------------------------------------------------------------------

/**
 * Controls the visual presentation of the open-private-chat entry point.
 *
 * Choose based on available screen space and context:
 * - [ICON_BUTTON] for tight row layouts (offerbook, trade header)
 * - [TEXT_BUTTON] for spacious contexts (bottom sheets, user profile cards)
 */
enum class OpenPrivateChatVariant {
    /**
     * A 32 dp circular icon button. Low visual weight, fits inline with avatars.
     * Shows a chat bubble icon. Loading state: replaces icon with a spinner.
     */
    ICON_BUTTON,

    /**
     * A full-width filled button with text label "Send private message".
     * Higher visual weight. Best for sheets and detail screens.
     * Loading state: button becomes disabled with spinner text.
     */
    TEXT_BUTTON,
}

// Production entry-point is not included in this design PoC —
// it will be created during implementation using the new UiState/UiAction pattern.

// ---------------------------------------------------------------------------
// Stateless content composable — used by previews and production
// ---------------------------------------------------------------------------

/**
 * Stateless implementation. Renders either variant based on [variant] parameter.
 *
 * @param peerName       Used in accessibility labels.
 * @param isOpeningChat  True while the channel is being created/fetched.
 * @param variant        ICON_BUTTON or TEXT_BUTTON.
 * @param onOpenChat     Click handler — triggers channel creation + navigation.
 */
@Composable
fun OpenPrivateChatContent(
    peerName: String,
    isOpeningChat: Boolean,
    variant: OpenPrivateChatVariant,
    onOpenChat: () -> Unit,
) {
    when (variant) {
        OpenPrivateChatVariant.ICON_BUTTON -> {
            OpenPrivateChatIconButton(
                peerName = peerName,
                isLoading = isOpeningChat,
                onClick = onOpenChat,
            )
        }
        OpenPrivateChatVariant.TEXT_BUTTON -> {
            OpenPrivateChatTextButton(
                isLoading = isOpeningChat,
                onClick = onOpenChat,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Icon button variant
// ---------------------------------------------------------------------------

/**
 * Icon button variant — the chat bubble icon button for inline placement.
 *
 * ## Sizing
 * The button's outer IconButton touch target is 48 dp (Android minimum).
 * The visible icon is 20 dp to appear proportional next to avatars.
 * The background circle is 32 dp with a semi-transparent primary colour fill,
 * making it visually connected to the Bisq green brand while not dominating.
 *
 * ## Loading state
 * When [isLoading] = true, the icon is replaced with a small (16 dp)
 * CircularProgressIndicator in the same primary green. The button is not
 * disabled during loading because the user cannot tap it again anyway
 * (the click already triggered — navigation will occur when loading finishes).
 *
 * ## Icon choice
 * Icons.Filled.ChatBubbleOutline communicates "send a message" clearly.
 * The outline style (not filled) avoids visual heaviness next to a filled avatar icon.
 *
 * @param peerName  Used in the content description for accessibility.
 * @param isLoading True while the backend channel operation is in flight.
 * @param onClick   Invoked when the user taps the button.
 */
@Composable
fun OpenPrivateChatIconButton(
    peerName: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDesc = "mobile.privateChats.openChat".i18n() + " $peerName"
    IconButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.semantics { contentDescription = contentDesc },
    ) {
        Surface(
            shape = CircleShape,
            color = BisqTheme.colors.primaryDisabled.copy(alpha = 0.25f),
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = BisqTheme.colors.primary,
                        strokeWidth = 1.5.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = BisqTheme.colors.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Text button variant
// ---------------------------------------------------------------------------

/**
 * Text button variant — a full-width "Send private message" button for sheets.
 *
 * Wraps the existing [BisqButton] atom. The button becomes disabled and shows
 * a spinner text suffix while [isLoading] is true.
 *
 * This variant is appropriate when there is enough space to explain what the
 * action does — important for first-time users who have never sent a private
 * message on Bisq.
 *
 * @param isLoading True while the backend channel operation is in flight.
 * @param onClick   Invoked when the user taps the button.
 */
@Composable
fun OpenPrivateChatTextButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BisqButton(
        text =
            if (isLoading) {
                "mobile.privateChats.openChat.loading".i18n() // "Opening..."
            } else {
                "mobile.privateChats.openChat".i18n() // "Send private message"
            },
        onClick = onClick,
        disabled = isLoading,
        modifier = modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

/**
 * Preview: icon button in its default idle state next to a simulated offer row.
 * Shows the button in context — inline with a peer avatar and username.
 */
@Preview
@Composable
private fun OpenPrivateChatButton_IconVariant_IdlePreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallRegular("In offer row context:", color = BisqTheme.colors.mid_grey20)

            // Simulated offer row with avatar + name + chat button
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(BisqTheme.colors.dark_grey40, shape = RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .padding(BisqUIConstants.ScreenPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Simulated avatar
                    Box(
                        Modifier.size(40.dp).background(BisqTheme.colors.dark_grey50, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        BisqText.SmallMedium("S", color = BisqTheme.colors.mid_grey30)
                    }
                    Column {
                        BisqText.BaseRegular("SatoshiFan#1234", color = BisqTheme.colors.white)
                        StarRating(rating = 4.5)
                    }
                }

                // The chat button — this is the component under design
                OpenPrivateChatIconButton(
                    peerName = "SatoshiFan#1234",
                    isLoading = false,
                    onClick = {},
                )
            }
        }
    }
}

/**
 * Preview: icon button while loading (channel creation in progress).
 * Shows the spinner replacing the chat icon.
 */
@Preview
@Composable
private fun OpenPrivateChatButton_IconVariant_LoadingPreview() {
    BisqTheme.Preview {
        Box(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).background(BisqTheme.colors.dark_grey50, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    BisqText.SmallMedium("B", color = BisqTheme.colors.mid_grey30)
                }
                BisqText.BaseRegular("BitcoinBee#5678", color = BisqTheme.colors.white)
                OpenPrivateChatIconButton(
                    peerName = "BitcoinBee#5678",
                    isLoading = true, // spinner shown
                    onClick = {},
                )
            }
        }
    }
}

/**
 * Preview: text button variant in idle state, as it would appear in a bottom sheet
 * or user profile detail card.
 */
@Preview
@Composable
private fun OpenPrivateChatButton_TextVariant_IdlePreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.dark_grey50)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            // Simulated profile card header
            Row(
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(56.dp).background(BisqTheme.colors.dark_grey30, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    BisqText.H4Light("C", color = BisqTheme.colors.mid_grey30)
                }
                Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
                    BisqText.H5Regular("CryptoNomad#9012", color = BisqTheme.colors.white)
                    StarRating(rating = 2.1)
                    BisqText.SmallLight("Reputation: 12,400 pts", color = BisqTheme.colors.mid_grey20)
                }
            }
            BisqGap.V1()
            // The chat button — full width in sheet context
            OpenPrivateChatTextButton(isLoading = false, onClick = {})
        }
    }
}

/**
 * Preview: text button variant while loading.
 */
@Preview
@Composable
private fun OpenPrivateChatButton_TextVariant_LoadingPreview() {
    BisqTheme.Preview {
        Box(
            modifier =
                Modifier
                    .background(BisqTheme.colors.dark_grey50)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            OpenPrivateChatTextButton(isLoading = true, onClick = {})
        }
    }
}

/**
 * Preview: both variants side by side for direct visual comparison.
 */
@Preview
@Composable
private fun OpenPrivateChatButton_BothVariantsComparisonPreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            BisqText.SmallRegular("ICON_BUTTON variant (for rows):", color = BisqTheme.colors.mid_grey20)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                BisqText.SmallLight("Idle:", color = BisqTheme.colors.mid_grey30)
                OpenPrivateChatIconButton("SatoshiFan#1234", false, {})
                BisqGap.H1()
                BisqText.SmallLight("Loading:", color = BisqTheme.colors.mid_grey30)
                OpenPrivateChatIconButton("SatoshiFan#1234", true, {})
            }

            HorizontalDivider(color = BisqTheme.colors.dark_grey50)

            BisqText.SmallRegular("TEXT_BUTTON variant (for sheets):", color = BisqTheme.colors.mid_grey20)
            OpenPrivateChatTextButton(false, {})
            OpenPrivateChatTextButton(true, {})
        }
    }
}

/**
 * Preview: isOwnProfile = true should render nothing.
 * This verifies the guard that prevents showing the button for the user's own avatar.
 */
@Preview
@Composable
private fun OpenPrivateChatButton_OwnProfile_RenderNothingPreview() {
    BisqTheme.Preview {
        Box(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                Box(
                    Modifier.size(40.dp).background(BisqTheme.colors.dark_grey50, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    BisqText.SmallMedium("M", color = BisqTheme.colors.primary)
                }
                BisqText.BaseRegular("MyOwnProfile#0001 (you)", color = BisqTheme.colors.white)
                // isOwnProfile = true → nothing rendered here
                // In production: OpenPrivateChatButton(peerProfileId, peerName, isOwnProfile = true)
                BisqText.SmallLight("[no button]", color = BisqTheme.colors.mid_grey20)
            }
        }
    }
}
