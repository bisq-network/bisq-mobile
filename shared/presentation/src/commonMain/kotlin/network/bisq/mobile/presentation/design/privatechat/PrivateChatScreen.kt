/**
 * # PrivateChatScreen
 *
 * The 1:1 private chat view for a single TwoPartyPrivateChatChannel (issue #590).
 *
 * ## Design goal
 * This screen should feel identical to the existing trade chat (TradeChatScreen),
 * with two deliberate differences:
 *   1. No trade-state header (no amount, no step indicator — this is not a trade)
 *   2. A peer profile header below the TopBar showing who you're chatting with
 *      (avatar + username + star rating). This compensates for the lack of trade
 *      context and keeps trust signals visible at all times.
 *
 * ## Desktop reference
 * Desktop (PrivateChatsView.java):
 * - The chat header shows a UserProfileDisplay (avatar + username + reputation) for
 *   the peer. This is critical — desktop makes it very visible.
 * - The ellipsis menu has a "Leave chat" option (red, destructive). On mobile
 *   we place "Leave chat" in the TopBar's trailing extraActions slot using a
 *   DropdownMenu or a dedicated icon button.
 * - The "no open chats" state on desktop is the whole chat area being disabled.
 *   On mobile this state cannot occur in this screen (you navigate here only
 *   when a channel exists) — it's handled by the PrivateChatListScreen.
 *
 * ## Layout (top to bottom)
 * ```
 * ┌─────────────────────────────────────────┐
 * │ TopBar: "← Private Messages" · [Leave]  │  44 dp  TopAppBar
 * ├─────────────────────────────────────────┤
 * │ PeerHeader: Avatar · Name · ★★★★☆       │  64 dp  (PeerChatHeader molecule)
 * ├─────────────────────────────────────────┤
 * │                                         │
 * │         ChatMessageList                 │  weight(1f)
 * │         (reverseLayout=true)            │
 * │                                         │
 * ├─────────────────────────────────────────┤
 * │ ChatInputField (+ QuotedMessage banner) │  wrap_content
 * └─────────────────────────────────────────┘
 * ```
 *
 * ## Reuse of existing components
 * - `ChatMessageList` — unchanged; accepts BisqEasyOpenTradeMessageModel list.
 *   Private chat messages (TwoPartyPrivateChatMessage) must be adapted via an
 *   extension function in the presenter: `msg.toBisqEasyOpenTradeMessageModel()`,
 *   mapping common fields (id, text, sender, date, reactions, citation) and leaving
 *   trade-specific fields null. This avoids modifying ChatMessageList.
 * - `ChatInputField` — unchanged; accepts quotedMessage + onMessageSend callback.
 * - `ConfirmationDialog` — used for leave-chat and ignore-user confirmations.
 * - `UndoIgnoreDialog` — reused from the trade chat organisms.
 * - `ReportUserDialog` — reused unchanged.
 * - `BisqStaticScaffold` — the layout wrapper used by TradeChatScreen.
 *
 * ## "Leave chat" action
 * On desktop this is a red menu item in the ellipsis menu. On mobile:
 * - A trailing `IconButton` in the TopBar's `extraActions` slot renders a
 *   leave / exit icon (e.g. `Icons.AutoMirrored.Filled.ExitToApp` or a
 *   custom `LeaveIcon` to be added to the icon atom library).
 * - Tapping it shows `ConfirmationDialog` with a warning-colour headline.
 * - Confirmed → presenter calls LeavePrivateChatManager.leaveChannel() then
 *   navigates back to PrivateChatList.
 *
 * Touch target: The icon button is 48 dp by default via IconButton.
 *
 * ## Navigation
 * Entry: NavRoute.PrivateChat(channelId)
 * Back:  Standard back → NavRoute.PrivateChatList
 * Leave: Navigates to NavRoute.PrivateChatList after channel deletion
 *
 * ## i18n keys
 * mobile.privateChats.chat.leaveChat      = "Leave chat"
 * mobile.privateChats.chat.leaveConfirm   = "Leave this private conversation? The chat history will be deleted for both parties."
 * mobile.privateChats.peer.header         = "Chat with {0}"
 *
 * ## Text expansion
 * "Chat with SatoshiFan#1234" in German: "Chat mit SatoshiFan#1234" (OK, same length).
 * "Leave chat" → "Chat verlassen" (30% longer) — the icon-only button in the
 * TopBar avoids this problem entirely.
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

// Production entry-point and PrivateChatContent composable are not included in this
// design PoC — they will be created during implementation using the new UiState/UiAction pattern.

// ---------------------------------------------------------------------------
// Leave chat icon button (atom helper)
// ---------------------------------------------------------------------------

/**
 * "Leave chat" icon button for the TopBar extraActions slot.
 *
 * Uses ExitToApp icon with danger tint to signal the destructive nature of
 * the action, but does NOT confirm immediately — tapping shows a dialog.
 * This two-step pattern prevents accidental channel deletion.
 *
 * Content description ensures screen reader accessibility ("Leave chat").
 */
@Composable
fun LeaveChatIconButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = "mobile.privateChats.chat.leaveChat".i18n() },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null, // described by parent semantics
            tint = BisqTheme.colors.danger,
            modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

/**
 * Preview: the peer chat header in isolation.
 * Shows avatar placeholder, username, and 4.5 star rating.
 */
@Preview
@Composable
private fun PeerChatHeader_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor)) {
            // Simulate the TopBar above it for context
            TopBarContent(
                title = "Chat with SatoshiFan#1234",
                showBackButton = true,
                extraActions = { LeaveChatIconButton(onClick = {}) },
            )
            // Simulated header (without real UserProfileVO/PlatformImage)
            SimulatedPeerChatHeader(
                name = "SatoshiFan#1234",
                stars = 4.5,
            )
        }
    }
}

/**
 * Preview: full chat screen with messages.
 *
 * Shows the complete layout: TopBar → PeerHeader → Messages → Input.
 * Messages are simulated as static text rows (no real ChatMessageList in preview)
 * to keep the PoC self-contained.
 */
@Preview
@Composable
private fun PrivateChatScreen_WithMessagesPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.background(BisqTheme.colors.backgroundColor),
        ) {
            TopBarContent(
                title = "Chat with SatoshiFan#1234",
                showBackButton = true,
                extraActions = { LeaveChatIconButton(onClick = {}) },
            )
            SimulatedPeerChatHeader(name = "SatoshiFan#1234", stars = 4.5)

            // Simulated message area (grey placeholder — represents ChatMessageList)
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(BisqTheme.colors.backgroundColor)
                        .padding(BisqUIConstants.ScreenPadding),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                    modifier = Modifier.align(Alignment.BottomStart),
                ) {
                    SimulatedInboundBubble("Hello! I saw your offer in the offerbook.")
                    SimulatedOutboundBubble("Hi! Yes, it's still available. Which payment method do you prefer?")
                    SimulatedInboundBubble("I'd like to use SEPA. Is that fine?")
                    SimulatedOutboundBubble("Perfect, SEPA works for me!")
                }
            }

            // Simulated input field
            SimulatedChatInputField()
        }
    }
}

/**
 * Preview: empty chat (first message not yet sent).
 * Shows the "no messages yet" state with just the header + input visible.
 */
@Preview
@Composable
private fun PrivateChatScreen_EmptyPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.background(BisqTheme.colors.backgroundColor),
        ) {
            TopBarContent(
                title = "Chat with SatoshiFan#1234",
                showBackButton = true,
                extraActions = { LeaveChatIconButton(onClick = {}) },
            )
            SimulatedPeerChatHeader(name = "SatoshiFan#1234", stars = 4.5)

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(BisqTheme.colors.backgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                BisqText.BaseLight(
                    text = "Send a message to start the conversation",
                    color = BisqTheme.colors.mid_grey20,
                )
            }

            SimulatedChatInputField()
        }
    }
}

/**
 * Preview: leave-chat confirmation dialog overlay.
 */
@Preview
@Composable
private fun PrivateChatScreen_LeaveChatDialogPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.background(BisqTheme.colors.backgroundColor),
        ) {
            TopBarContent(
                title = "Chat with SatoshiFan#1234",
                showBackButton = true,
                extraActions = { LeaveChatIconButton(onClick = {}) },
            )
            SimulatedPeerChatHeader(name = "SatoshiFan#1234", stars = 4.5)
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(BisqTheme.colors.backgroundColor))
            SimulatedChatInputField()

            // Dialog overlay
            ConfirmationDialog(
                headline = "Leave chat",
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = "Leave this private conversation? The chat history will be deleted for both parties.",
                confirmButtonText = "Yes, leave",
                dismissButtonText = "Cancel",
                verticalButtonPlacement = true,
                onConfirm = {},
                onDismiss = { _ -> },
            )
        }
    }
}

/**
 * Preview: the leave icon button in isolation.
 */
@Preview
@Composable
private fun LeaveChatIconButton_Preview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "Chat with BitcoinBee#5678",
            showBackButton = true,
            extraActions = { LeaveChatIconButton(onClick = {}) },
        )
    }
}

// ---------------------------------------------------------------------------
// Preview helpers (no domain types needed)
// ---------------------------------------------------------------------------

@Composable
private fun SimulatedPeerChatHeader(
    name: String,
    stars: Double,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor)
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(BisqUIConstants.topBarAvatarSize)
                    .background(BisqTheme.colors.dark_grey50, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BisqText.SmallMedium(text = name.first().toString(), color = BisqTheme.colors.mid_grey30)
        }
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
            BisqText.BaseRegular(text = name, color = BisqTheme.colors.white, singleLine = true)
            StarRating(rating = stars)
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = BisqTheme.colors.dark_grey50)
}

@Composable
private fun SimulatedInboundBubble(text: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
        Box(
            Modifier.size(28.dp).background(BisqTheme.colors.dark_grey50, shape = CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .background(BisqTheme.colors.dark_grey40, shape = RoundedCornerShape(12.dp))
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.BaseLight(text = text, color = BisqTheme.colors.white)
        }
    }
}

@Composable
private fun SimulatedOutboundBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier =
                Modifier
                    .background(BisqTheme.colors.primaryDisabled, shape = RoundedCornerShape(12.dp))
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.BaseLight(text = text, color = BisqTheme.colors.white)
        }
    }
}

@Composable
private fun SimulatedChatInputField() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.secondary)
                .padding(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .background(BisqTheme.colors.dark_grey40, shape = RoundedCornerShape(BisqUIConstants.textFieldBorderRadius))
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.BaseLight(text = "Write a message...", color = BisqTheme.colors.mid_grey20)
        }
    }
}
