/**
 * PrivateChatScreenDesign.kt — Design PoC (Milestone 11 "Bisq community", issue #590)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * REVISION NOTE (refresh pass)
 * ======================================================================================
 * Originally authored 2026-06-11 as `design/privatechat/PrivateChatScreen.kt`, before
 * this milestone's community IA was decided. Relocated under `design/community/
 * private_chat/` and refreshed:
 *   - Adopted the `PrivateChatUiState` + `PrivateChatUiAction` pattern (this file
 *     previously had no single content composable — production and previews are now
 *     driven through `PrivateChatScreenContent`, matching every other file in the set).
 *   - `internal` visibility on all top-level declarations; `@ExcludeFromCoverage` added
 *     to every preview.
 *   - `Modifier.testTag` instead of `semantics { contentDescription }` on
 *     `LeaveChatIconButton`.
 *   - The simulated input field is replaced with the REAL production `ChatInputField`
 *     (molecules/chat/ChatInputField.kt) — its `quotedMessage` param defaults to null,
 *     so it needs no `BisqEasyOpenTradeMessageModel` instance to preview, exactly the
 *     same reuse trick already used in DiscussionsChannelScreenDesign.kt. This is the
 *     concrete "align styling with the community components" fix rodvar asked for.
 *
 * This screen is what PeerProfileScreenDesign.kt's "Send private message" button
 * (`OpenPrivateChatTextButton`, see OpenPrivateChatButtonDesign.kt in this package)
 * navigates to once #590 ships — this milestone that button stays disabled with a
 * "coming soon" label; this screen documents the destination it targets.
 *
 * ======================================================================================
 * DESIGN GOAL
 * ======================================================================================
 * This screen should feel identical to the existing trade chat (TradeChatScreen),
 * with two deliberate differences:
 *   1. No trade-state header (no amount, no step indicator — this is not a trade)
 *   2. A peer profile header below the TopBar showing who you're chatting with
 *      (avatar + username + star rating). This compensates for the lack of trade
 *      context and keeps trust signals visible at all times. Tapping this header
 *      should navigate to the Peer Profile screen (#545) — same generalized
 *      peer-identity tap-target requirement documented across the community set.
 *
 * ======================================================================================
 * DESKTOP REFERENCE
 * ======================================================================================
 * Desktop (PrivateChatsView.java):
 * - The chat header shows a UserProfileDisplay (avatar + username + reputation) for
 *   the peer. This is critical — desktop makes it very visible.
 * - The ellipsis menu has a "Leave chat" option (red, destructive). On mobile
 *   we place "Leave chat" in the TopBar's trailing extraActions slot using a
 *   dedicated icon button.
 * - The "no open chats" state on desktop is the whole chat area being disabled.
 *   On mobile this state cannot occur in this screen (you navigate here only
 *   when a channel exists) — it's handled by PrivateChatListScreenDesign.kt.
 *
 * ======================================================================================
 * LAYOUT (top to bottom)
 * ======================================================================================
 * ┌─────────────────────────────────────────┐
 * │ TopBar: "← Chat with SatoshiFan#1234" ⛔ │  ⛔ = leave chat
 * ├─────────────────────────────────────────┤
 * │ PeerHeader: Avatar · Name · ★★★★☆        │  tappable → Peer Profile
 * ├─────────────────────────────────────────┤
 * │                                         │
 * │         Messages (reverse layout)        │
 * │                                         │
 * ├─────────────────────────────────────────┤
 * │ ChatInputField (real component)          │
 * └─────────────────────────────────────────┘
 *
 * ======================================================================================
 * REUSE OF EXISTING COMPONENTS
 * ======================================================================================
 * - `ChatMessageList` — unchanged in production; accepts BisqEasyOpenTradeMessageModel
 *   list. Private chat messages (TwoPartyPrivateChatMessage) must be adapted via an
 *   extension function in the presenter: `msg.toBisqEasyOpenTradeMessageModel()`,
 *   mapping common fields (id, text, sender, date, reactions, citation) and leaving
 *   trade-specific fields null. This avoids modifying ChatMessageList. (NOTE: this is a
 *   DIFFERENT adapter from the one public channels need — see DiscussionsChannelScreen
 *   Design.kt's REUSE MAP section for why the two can't share one mapping function.)
 * - `ChatInputField` — reused directly in THIS PoC (see revision note above).
 * - `ConfirmationDialog` — used for leave-chat confirmation.
 * - `BisqStaticScaffold` — the layout wrapper used by TradeChatScreen, used in
 *   production for this screen too.
 *
 * ======================================================================================
 * "LEAVE CHAT" ACTION
 * ======================================================================================
 * On desktop this is a red menu item in the ellipsis menu. On mobile:
 * - A trailing `IconButton` in the TopBar's `extraActions` slot renders a leave/exit
 *   icon with danger tint.
 * - Tapping it shows `ConfirmationDialog` with a warning-colour headline.
 * - Confirmed → presenter leaves the channel, then navigates back to
 *   PrivateChatListScreenDesign.kt's screen.
 *
 * This is DELIBERATELY NOT present on DiscussionsChannelScreenDesign.kt — a public
 * channel is not "left" in the same destructive sense a 1:1 DM is. Do not copy this
 * icon button into the channel screen; see that file's KDoc for the explicit contrast.
 *
 * Touch target: the icon button is 48 dp by default via IconButton.
 *
 * ======================================================================================
 * i18n KEYS NEEDED
 * ======================================================================================
 * mobile.privateChats.chat.leaveChat      = "Leave chat"
 * mobile.privateChats.chat.leaveConfirm   = "Leave this private conversation? The chat history will be deleted for both parties."
 * mobile.privateChats.peer.header         = "Chat with {0}"
 * mobile.privateChats.chat.emptyHint      = "Send a message to start the conversation"
 *
 * ======================================================================================
 * TEXT EXPANSION
 * ======================================================================================
 * "Chat with SatoshiFan#1234" in German: "Chat mit SatoshiFan#1234" (OK, same length).
 * "Leave chat" → "Chat verlassen" (30% longer) — the icon-only button in the
 * TopBar avoids this problem entirely.
 */
package network.bisq.mobile.presentation.design.community.private_chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ============================================================================================
// Simulated data — no domain type dependencies
// ============================================================================================

internal data class SimulatedPrivateChatMessage(
    val id: String,
    val text: String,
    val isMine: Boolean,
)

// ============================================================================================
// UiState / UiAction
// ============================================================================================

internal data class PrivateChatUiState(
    val peerName: String,
    val peerStarRating: Double,
    val messages: List<SimulatedPrivateChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val showLeaveConfirmDialog: Boolean = false,
)

internal sealed interface PrivateChatUiAction {
    data object OnPeerHeaderClick : PrivateChatUiAction

    data object OnLeaveClick : PrivateChatUiAction

    data object OnConfirmLeave : PrivateChatUiAction

    data object OnDismissLeaveDialog : PrivateChatUiAction

    data class OnSendMessage(
        val text: String,
    ) : PrivateChatUiAction
}

// ============================================================================================
// Leave chat icon button (reusable across previews and production)
// ============================================================================================

/**
 * "Leave chat" icon button for the TopBar extraActions slot.
 *
 * Uses ExitToApp icon with danger tint to signal the destructive nature of
 * the action, but does NOT confirm immediately — tapping shows a dialog.
 * This two-step pattern prevents accidental channel deletion.
 */
@Composable
internal fun LeaveChatIconButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag("leave_private_chat_button"),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null,
            tint = BisqTheme.colors.danger,
            modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
        )
    }
}

// ============================================================================================
// Content
// ============================================================================================

@Composable
internal fun PrivateChatScreenContent(
    uiState: PrivateChatUiState,
    onAction: (PrivateChatUiAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(BisqTheme.colors.backgroundColor)) {
        TopBarContent(
            title = "Chat with ${uiState.peerName}",
            showBackButton = true,
            extraActions = { LeaveChatIconButton(onClick = { onAction(PrivateChatUiAction.OnLeaveClick) }) },
        )
        SimulatedPeerChatHeader(
            name = uiState.peerName,
            stars = uiState.peerStarRating,
            onClick = { onAction(PrivateChatUiAction.OnPeerHeaderClick) },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(BisqUIConstants.ScreenPadding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        color = BisqTheme.colors.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                uiState.messages.isEmpty() -> {
                    BisqText.BaseLight(
                        text = "Send a message to start the conversation",
                        color = BisqTheme.colors.mid_grey20,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                    ) {
                        uiState.messages.forEach { message ->
                            if (message.isMine) {
                                SimulatedOutboundBubble(message.text)
                            } else {
                                SimulatedInboundBubble(message.text)
                            }
                        }
                    }
                }
            }
        }

        ChatInputField(
            onMessageSend = { text -> onAction(PrivateChatUiAction.OnSendMessage(text)) },
            placeholder = "Write a message...",
        )

        if (uiState.showLeaveConfirmDialog) {
            ConfirmationDialog(
                headline = "Leave chat",
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = "Leave this private conversation? The chat history will be deleted for both parties.",
                confirmButtonText = "Yes, leave",
                dismissButtonText = "Cancel",
                verticalButtonPlacement = true,
                onConfirm = { onAction(PrivateChatUiAction.OnConfirmLeave) },
                onDismiss = { _ -> onAction(PrivateChatUiAction.OnDismissLeaveDialog) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview helpers (no domain types needed)
// ---------------------------------------------------------------------------

@Composable
private fun SimulatedPeerChatHeader(
    name: String,
    stars: Double,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor)
                .clickable(onClick = onClick)
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf)
                .testTag("private_chat_peer_header"),
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

// ============================================================================================
// Preview fixtures
// ============================================================================================

private fun simulatedPrivateChatMessages() =
    listOf(
        SimulatedPrivateChatMessage(id = "1", text = "Hello! I saw your offer in the offerbook.", isMine = false),
        SimulatedPrivateChatMessage(id = "2", text = "Hi! Yes, it's still available. Which payment method do you prefer?", isMine = true),
        SimulatedPrivateChatMessage(id = "3", text = "I'd like to use SEPA. Is that fine?", isMine = false),
        SimulatedPrivateChatMessage(id = "4", text = "Perfect, SEPA works for me!", isMine = true),
    )

// ============================================================================================
// Previews
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "Private chat — Populated")
@Composable
private fun PrivateChatScreen_WithMessagesPreview() {
    BisqTheme.Preview {
        PrivateChatScreenContent(
            uiState = PrivateChatUiState(peerName = "SatoshiFan#1234", peerStarRating = 4.5, messages = simulatedPrivateChatMessages()),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Private chat — Empty (first message not sent yet)")
@Composable
private fun PrivateChatScreen_EmptyPreview() {
    BisqTheme.Preview {
        PrivateChatScreenContent(
            uiState = PrivateChatUiState(peerName = "SatoshiFan#1234", peerStarRating = 4.5, messages = emptyList()),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Private chat — Loading")
@Composable
private fun PrivateChatScreen_LoadingPreview() {
    BisqTheme.Preview {
        PrivateChatScreenContent(
            uiState = PrivateChatUiState(peerName = "SatoshiFan#1234", peerStarRating = 4.5, isLoading = true),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Private chat — Leave chat confirmation dialog")
@Composable
private fun PrivateChatScreen_LeaveChatDialogPreview() {
    BisqTheme.Preview {
        PrivateChatScreenContent(
            uiState =
                PrivateChatUiState(
                    peerName = "SatoshiFan#1234",
                    peerStarRating = 4.5,
                    messages = simulatedPrivateChatMessages(),
                    showLeaveConfirmDialog = true,
                ),
            onAction = {},
        )
    }
}

/**
 * Preview: the peer chat header in isolation, shown with the TopBar for context.
 */
@ExcludeFromCoverage
@Preview(name = "Private chat — Peer header isolated")
@Composable
private fun PeerChatHeader_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor)) {
            TopBarContent(
                title = "Chat with SatoshiFan#1234",
                showBackButton = true,
                extraActions = { LeaveChatIconButton(onClick = {}) },
            )
            SimulatedPeerChatHeader(name = "SatoshiFan#1234", stars = 4.5, onClick = {})
        }
    }
}

/**
 * Preview: the leave icon button in isolation.
 */
@ExcludeFromCoverage
@Preview(name = "Private chat — Leave icon button isolated")
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
