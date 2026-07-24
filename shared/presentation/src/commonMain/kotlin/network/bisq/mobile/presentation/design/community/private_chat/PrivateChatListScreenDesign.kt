/**
 * PrivateChatListScreenDesign.kt — Design PoC (Milestone 11 "Bisq community", issue #590)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * REVISION NOTE (refresh pass)
 * ======================================================================================
 * Originally authored 2026-06-11 as `design/privatechat/PrivateChatListScreen.kt`,
 * before this milestone's community IA was decided — its original KDoc argued for a
 * More-menu entry and against a 5th bottom-nav tab, reachable only via an ad-hoc route.
 * That conclusion is SUPERSEDED (see project_milestone11_community_ia.md agent memory):
 * this screen is now the **Messages segment of the Community hub**
 * (CommunityHubScreenDesign.kt) — see the "ROLE IN THE HUB" section below, which is the
 * concrete decision rodvar made for how #590 slots into the milestone-11 IA.
 *
 * Also refreshed to current PoC conventions:
 *   - This file previously had NO reusable content composable — the "populated" preview
 *     built the whole list inline in the preview function body. Extracted into
 *     `PrivateChatListScreenContent(uiState, onAction)` + `PrivateChatListUiState` /
 *     `PrivateChatListUiAction`, matching every other file in the set.
 *   - `internal` visibility on all top-level declarations; `@ExcludeFromCoverage` added
 *     to every preview.
 *   - `ConversationRow`'s unread indicator now uses the SAME manual pill styling as
 *     `ChannelRow` in CommunityHubScreenDesign.kt (a plain `Box` + `BisqText.XSmallMedium`
 *     pill) instead of the previous ad-hoc `Badge`/`BadgedBox` — so a Discussions channel
 *     row and a DM conversation row read as visual siblings within the same hub, per
 *     rodvar's "align styling with your new community components" note. This also avoids
 *     the `BadgedBox` clipping failure mode fixed in `CommunityEntryPointDesign.kt`
 *     (see that file's KDoc) — the manual pill is a plain `Box`, nothing to clip.
 *   - `simulatedConversations()` is now `internal` (was `private`) specifically so
 *     `CommunityHubScreenDesign.kt`'s future-segmented-hub preview can reuse the exact
 *     same fixture data for its Messages tab, instead of duplicating a second fixture.
 *
 * ======================================================================================
 * ROLE IN THE HUB (decision, documented here per rodvar's request)
 * ======================================================================================
 * Per the milestone-11 IA: the Community hub has three logical segments —
 * **Discussions** (public channels, #589, shipping this milestone) · **Messages**
 * (this screen — the DM inbox, #590, fast-follow) · **Contacts** (#1238, fast-follow,
 * a relationship directory, NOT a message list — do not confuse the two).
 *
 * Private 1:1 DMs and public channels are deliberately kept in SEPARATE segments, not
 * merged into one combined list. They are distinct objects with different lifecycles:
 * a DM is a persistent relationship with one specific peer (has a "leave chat" — see
 * PrivateChatScreenDesign.kt); a public channel is an implicit, non-exclusive membership
 * in a many-to-many space (no "leave"). Interleaving them in one list would blur that
 * distinction and make the "who is this conversation with" question harder to answer
 * at a glance — exactly the trust-clarity problem this app's whole design philosophy
 * exists to avoid.
 *
 * See CommunityHubScreenDesign.kt's "CANONICAL DESIGN, GATED ROLLOUT" section — and its
 * `CommunityHubScreen_TwoSegmentsLivePreview` and
 * `CommunityHubScreen_AllSegmentsLiveInteractivePreview` previews — for this screen
 * mounted live as the Messages tab. The hub is one canonical 3-tab design now, not a
 * separate "future" screen.
 *
 * ======================================================================================
 * DESKTOP REFERENCE
 * ======================================================================================
 * The desktop equivalent (bisq2/.../chat/priv/PrivateChatsView.java) shows a
 * 210 px wide left-panel sidebar listing open chats, with the chat pane on the right.
 * On mobile we adapt this to a conventional **master → detail** pattern:
 * this screen is the master (list); PrivateChatScreenDesign.kt is the detail.
 *
 * ======================================================================================
 * LAYOUT DECISIONS
 * ======================================================================================
 * - **List item** = peer avatar (48 dp) + username + star rating + last-message preview
 *   + relative timestamp + unread pill. Mirrors ChannelRow's shape in
 *   CommunityHubScreenDesign.kt for the "hub row" visual family described above.
 * - **Empty state** uses the same card+CTA pattern as NoTradesSection in
 *   OpenTradeListScreen for visual consistency, and directs the user to the Offerbook
 *   (where peer avatars appear) rather than leaving a dead end.
 * - **Loading state** uses CircularProgressIndicator on a centred Box, matching
 *   OpenTradeListScreen.
 *
 * ======================================================================================
 * i18n KEYS NEEDED
 * ======================================================================================
 * mobile.privateChats.list.title       → "Private Messages" (this milestone's hub
 *   segment label is "Messages" — see CommunityHubScreenDesign.kt; this key is for the
 *   screen's own TopBar title when reached directly, e.g. via deep link)
 * mobile.privateChats.list.empty       → "No private conversations yet"
 * mobile.privateChats.list.empty.hint  → "Tap any user avatar in the Offerbook to start a private conversation"
 *
 * ======================================================================================
 * TEXT EXPANSION
 * ======================================================================================
 * "Private Messages" in German is "Private Nachrichten" (30% longer).
 * The TopBar uses AutoResizeText with maxLines=2, so this is handled.
 * List item username uses singleLine = true with ellipsis truncation.
 * Last-message preview is capped at 1 line via `singleLine = true`.
 *
 * ======================================================================================
 * IMPLEMENTATION NOTES
 * ======================================================================================
 * - Unread counts: create PrivateChatReadStateRepository (same pattern as
 *   TradeReadStateRepository, keyed by channelId).
 * - Push notifications: deep-link to bisq://PrivateChat/{channelId}.
 * - Blocked users: hide OpenPrivateChatButton (see OpenPrivateChatButtonDesign.kt) when
 *   the peer is in ignoredProfileIds.
 * - Performance: consider lazy-loading messages and paginating after 50 conversations.
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ============================================================================================
// Simulated data — no domain type dependencies
// ============================================================================================

internal data class SimulatedConversation(
    val id: String,
    val name: String,
    val lastMessagePreview: String,
    val lastMessageTime: String,
    val starRating: Double,
    val unreadCount: Int,
)

// ============================================================================================
// UiState / UiAction
// ============================================================================================

internal data class PrivateChatListUiState(
    val conversations: List<SimulatedConversation> = emptyList(),
    val isLoading: Boolean = false,
)

internal sealed interface PrivateChatListUiAction {
    data class OnConversationClick(
        val conversationId: String,
    ) : PrivateChatListUiAction

    data object OnBrowseOfferbookClick : PrivateChatListUiAction
}

// ============================================================================================
// Content
// ============================================================================================

@Composable
internal fun PrivateChatListScreenContent(
    uiState: PrivateChatListUiState,
    onAction: (PrivateChatListUiAction) -> Unit,
    showTopBar: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxSize().background(BisqTheme.colors.backgroundColor)) {
        if (showTopBar) {
            TopBarContent(title = "Private Messages", showBackButton = true)
        }

        when {
            uiState.isLoading -> PrivateChatListLoadingState()
            uiState.conversations.isEmpty() -> {
                PrivateChatListEmptyState(onNavigateToOfferbook = { onAction(PrivateChatListUiAction.OnBrowseOfferbookClick) })
            }
            else -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    uiState.conversations.forEach { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick = { onAction(PrivateChatListUiAction.OnConversationClick(conversation.id)) },
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = BisqTheme.colors.dark_grey50,
                            modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Loading state
// ---------------------------------------------------------------------------

@Composable
private fun PrivateChatListLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = BisqTheme.colors.primary,
            strokeWidth = 2.dp,
        )
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

/**
 * Shown when the user has no private conversations yet.
 *
 * Design rationale: The user needs to understand WHERE to find peers to chat with.
 * Instead of just saying "nothing here", we direct them to the Offerbook where
 * peer avatars appear. This reduces dead-ends for new users.
 */
@Composable
private fun PrivateChatListEmptyState(onNavigateToOfferbook: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = BisqUIConstants.ScreenPadding2X),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding4X),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding3X),
        ) {
            BisqText.H4LightGrey(
                text = "No private conversations yet",
                textAlign = TextAlign.Center,
            )
            BisqText.BaseLight(
                text = "Tap any user avatar in the Offerbook to start a private conversation",
                color = BisqTheme.colors.mid_grey20,
                textAlign = TextAlign.Center,
            )
            BisqGap.V1()
            BisqButton(
                text = "Browse Offerbook",
                onClick = onNavigateToOfferbook,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Conversation row
// ---------------------------------------------------------------------------

@Composable
private fun ConversationRow(
    conversation: SimulatedConversation,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalfQuarter),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(BisqUIConstants.ScreenPadding4X)
                    .background(BisqTheme.colors.dark_grey50, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BisqText.SmallMedium(text = conversation.name.first().toString(), color = BisqTheme.colors.mid_grey30)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BisqText.BaseRegular(
                    text = conversation.name,
                    color = BisqTheme.colors.white,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                BisqText.SmallRegular(text = conversation.lastMessageTime, color = BisqTheme.colors.mid_grey20)
            }
            StarRating(rating = conversation.starRating)
            BisqText.SmallLight(text = conversation.lastMessagePreview, color = BisqTheme.colors.mid_grey30)
        }

        // Same manual pill shape as ChannelRow in CommunityHubScreenDesign.kt — a plain
        // Box, not a BadgedBox/IconButton pairing, so it has no clipping risk (see the
        // badge-clipping bug fixed in CommunityEntryPointDesign.kt).
        if (conversation.unreadCount > 0) {
            Box(
                modifier =
                    Modifier
                        .background(BisqTheme.colors.primary, shape = CircleShape)
                        .padding(horizontal = BisqUIConstants.ScreenPaddingHalf, vertical = BisqUIConstants.ScreenPaddingQuarter),
            ) {
                BisqText.XSmallMedium(text = conversation.unreadCount.toString(), color = BisqTheme.colors.white)
            }
        }
    }
}

// ============================================================================================
// Preview fixtures
// ============================================================================================

/**
 * Shared fixture — kept `internal` (not `private`) so
 * CommunityHubScreenDesign.kt's future-segmented-hub preview can reuse the same
 * conversation data for its Messages tab rather than duplicating a second fixture.
 */
internal fun simulatedConversations() =
    listOf(
        SimulatedConversation(
            id = "1",
            name = "SatoshiFan#1234",
            lastMessagePreview = "Sure, let me know when you're ready.",
            lastMessageTime = "2 min",
            starRating = 4.5,
            unreadCount = 3,
        ),
        SimulatedConversation(
            id = "2",
            name = "BitcoinBee#5678",
            lastMessagePreview = "You: Thanks for the quick response!",
            lastMessageTime = "3 h",
            starRating = 3.8,
            unreadCount = 0,
        ),
        SimulatedConversation(
            id = "3",
            name = "CryptoNomad#9012",
            lastMessagePreview = "Hello! I saw your offer in the offerbook.",
            lastMessageTime = "Yesterday",
            starRating = 2.1,
            unreadCount = 1,
        ),
        SimulatedConversation(
            id = "4",
            name = "PeerNode#3456",
            lastMessagePreview = "What payment methods do you accept?",
            lastMessageTime = "Mon",
            starRating = 4.9,
            unreadCount = 0,
        ),
    )

// ============================================================================================
// Previews
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "Private Messages — Populated")
@Composable
private fun PrivateChatListScreen_PopulatedPreview() {
    BisqTheme.Preview {
        PrivateChatListScreenContent(
            uiState = PrivateChatListUiState(conversations = simulatedConversations()),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Private Messages — Empty")
@Composable
private fun PrivateChatListScreen_EmptyPreview() {
    BisqTheme.Preview {
        PrivateChatListScreenContent(
            uiState = PrivateChatListUiState(conversations = emptyList()),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Private Messages — Loading")
@Composable
private fun PrivateChatListScreen_LoadingPreview() {
    BisqTheme.Preview {
        PrivateChatListScreenContent(
            uiState = PrivateChatListUiState(isLoading = true),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Conversation row — with unread pill")
@Composable
private fun ConversationRow_WithUnreadPreview() {
    BisqTheme.Preview {
        ConversationRow(conversation = simulatedConversations()[0], onClick = {})
    }
}

@ExcludeFromCoverage
@Preview(name = "Conversation row — no unread")
@Composable
private fun ConversationRow_NoUnreadPreview() {
    BisqTheme.Preview {
        ConversationRow(conversation = simulatedConversations()[1], onClick = {})
    }
}
