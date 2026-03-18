/**
 * # PrivateChatListScreen
 *
 * The entry-point screen for the "Private chats outside of trade" feature (issue #590).
 * Shows all open 1:1 private conversations the user has with other Bisq users, sorted
 * by most-recent message first.
 *
 * ## Where this lives in the app
 *
 * The screen is reachable from the **"More" tab** (TabMiscItems), listed alongside
 * Reputation, Settings, etc. It does NOT get its own bottom-nav tab because private
 * chats are a power-user feature and adding a 5th tab would crowd the bottom nav.
 * If usage analytics later show it's frequently accessed, it can be promoted.
 *
 * Navigation graph additions required:
 * ```
 * // In NavRoute.kt:
 * @Serializable data object PrivateChatList : NavRoute
 * @Serializable data class PrivateChat(val channelId: String) : NavRoute, DeepLinkableRoute
 * ```
 *
 * ## Desktop reference
 *
 * The desktop equivalent (bisq2/.../chat/priv/PrivateChatsView.java) shows a
 * 210 px wide left-panel sidebar listing open chats, with the chat pane on the right.
 * On mobile we adapt this to a conventional **master → detail** pattern:
 * this screen is the master (list); PrivateChatScreen is the detail.
 *
 * ## Layout decisions
 *
 * - **List item** = peer avatar (48 dp) + username + star rating + last-message preview
 *   + relative timestamp + unread badge. This maps directly to UserProfileRow
 *   (existing molecule) plus additional metadata.
 * - **Empty state** uses the same card+CTA pattern as NoTradesSection in
 *   OpenTradeListScreen for visual consistency.
 * - **Loading state** uses CircularProgressIndicator on a centred Box, matching
 *   OpenTradeListScreen.
 * - **Unread badge** follows the same AnimatedBadge pattern used in UserProfileRow.
 * - The TopBar title is static ("Private Messages") with no extra actions.
 *   The user's own avatar remains in the top-right corner (standard TopBar behaviour).
 *
 * ## Text expansion
 *
 * "Private Messages" in German is "Private Nachrichten" (30% longer).
 * The TopBar uses AutoResizeText with maxLines=2, so this is handled.
 * List item username uses singleLine = true with ellipsis truncation at 180 dp.
 * Last-message preview is capped at 2 lines.
 *
 * ## MVP pattern
 *
 * The composable is fully stateless. Previews use fake data structs.
 * Production entry-point and presenter interfaces are not included in this design PoC —
 * they will be created during implementation using the new UiState/UiAction pattern.
 *
 * ## Implementation notes
 *
 * - Unread counts: create PrivateChatReadStateRepository (same pattern as TradeReadStateRepository,
 *   keyed by channelId).
 * - "More" tab badge: TabMiscItemsPresenter should observe private chat unread counts.
 * - Push notifications: deep-link to bisq://PrivateChat/{channelId}.
 * - Blocked users: hide OpenPrivateChatButton when peer is in ignoredProfileIds.
 * - Performance: consider lazy-loading messages and paginating after 50 conversations.
 */
package network.bisq.mobile.presentation.design.privatechat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

// ---------------------------------------------------------------------------
// Reusable composables — these will be used by the production screen
// ---------------------------------------------------------------------------

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
 *
 * The padded card + centred text pattern mirrors NoTradesSection in
 * OpenTradeListScreen for visual consistency across the "empty list" states.
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
            // Primary message — what the screen is for
            BisqText.H4LightGrey(
                text = "mobile.privateChats.list.empty".i18n(),
                // "No private conversations yet"
                textAlign = TextAlign.Center,
            )

            // Secondary hint — where to go to find peers
            BisqText.BaseLight(
                text = "mobile.privateChats.list.empty.hint".i18n(),
                // "Tap any user avatar in the Offerbook to start a private conversation"
                color = BisqTheme.colors.mid_grey20,
                textAlign = TextAlign.Center,
            )

            BisqGap.V1()

            // CTA: guides users to the place where they can find peers
            BisqButton(
                text = "mobile.bisqEasy.tradeWizard.selectOffer.noMatchingOffers.browseOfferbook".i18n(),
                // "Browse Offerbook"
                onClick = onNavigateToOfferbook,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

/**
 * Preview: populated list with mix of read and unread conversations.
 * Uses SimulatedConversationRow to avoid needing real UserProfileVO instances.
 */
@Preview
@Composable
private fun PrivateChatListScreen_PopulatedPreview() {
    BisqTheme.Preview {
        // In production, replace with actual PrivateChatConversationVO instances
        // using createMockUserProfile() from domain test helpers.
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            TopBarContent(
                title = "Private Messages",
                showBackButton = true,
            )

            // Simulated list rows (without real UserProfileVO):
            repeat(4) { index ->
                SimulatedConversationRow(
                    name = listOf("SatoshiFan#1234", "BitcoinBee#5678", "CryptoNomad#9012", "PeerNode#3456")[index],
                    preview =
                        listOf(
                            "Sure, let me know when you're ready.",
                            "You: Thanks for the quick response!",
                            "Hello! I saw your offer in the offerbook.",
                            "What payment methods do you accept?",
                        )[index],
                    time = listOf("2 min", "3 h", "Yesterday", "Mon")[index],
                    stars = listOf(4.5, 3.8, 2.1, 4.9)[index],
                    unread = listOf(3, 0, 1, 0)[index],
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

/**
 * Preview: empty state when user has no conversations.
 */
@Preview
@Composable
private fun PrivateChatListScreen_EmptyPreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor),
        ) {
            TopBarContent(title = "Private Messages", showBackButton = true)
            PrivateChatListEmptyState(onNavigateToOfferbook = {})
        }
    }
}

/**
 * Preview: loading state (initial fetch from backend).
 */
@Preview
@Composable
private fun PrivateChatListScreen_LoadingPreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor),
        ) {
            TopBarContent(title = "Private Messages", showBackButton = true)
            PrivateChatListLoadingState()
        }
    }
}

/**
 * Preview: single row with unread badge — isolated component preview.
 */
@Preview
@Composable
private fun PrivateChatConversationRow_WithBadgePreview() {
    BisqTheme.Preview {
        SimulatedConversationRow(
            name = "SatoshiFan#1234",
            preview = "Sure, let me know when you're ready to proceed.",
            time = "2 min",
            stars = 4.5,
            unread = 3,
        )
    }
}

/**
 * Preview: single row with no unread messages.
 */
@Preview
@Composable
private fun PrivateChatConversationRow_NoUnreadPreview() {
    BisqTheme.Preview {
        SimulatedConversationRow(
            name = "BitcoinBee#5678",
            preview = "You: Thanks for the quick response! Really appreciate it.",
            time = "3 h",
            stars = 3.8,
            unread = 0,
        )
    }
}

/**
 * Simulated row for use in Previews only — avoids needing real UserProfileVO/PlatformImage
 * in the PoC. Production code should use PrivateChatConversationRow directly.
 */
@Composable
private fun SimulatedConversationRow(
    name: String,
    preview: String,
    time: String,
    stars: Double,
    unread: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalfQuarter),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar placeholder
        BadgedBox(
            modifier = Modifier.size(BisqUIConstants.ScreenPadding4X),
            badge = {
                if (unread > 0) {
                    Badge(containerColor = BisqTheme.colors.primary) {
                        BisqText.SmallMedium(
                            text = unread.toString(),
                            color = BisqTheme.colors.white,
                        )
                    }
                }
            },
        ) {
            Box(
                modifier =
                    Modifier
                        .size(BisqUIConstants.ScreenPadding4X)
                        .clip(RoundedCornerShape(BisqUIConstants.ScreenPadding2X))
                        .background(BisqTheme.colors.dark_grey50),
                contentAlignment = Alignment.Center,
            ) {
                BisqText.SmallMedium(text = name.first().toString(), color = BisqTheme.colors.mid_grey30)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BisqText.BaseRegular(text = name, color = BisqTheme.colors.white, singleLine = true, modifier = Modifier.weight(1f))
                BisqText.SmallRegular(text = time, color = BisqTheme.colors.mid_grey20)
            }
            StarRating(rating = stars)
            BisqText.SmallLight(text = preview, color = BisqTheme.colors.mid_grey30)
        }
    }
}
