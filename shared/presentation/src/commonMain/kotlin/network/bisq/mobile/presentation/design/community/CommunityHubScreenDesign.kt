/**
 * CommunityHubScreenDesign.kt — Design PoC (Milestone 11 "Bisq community")
 *
 * STATUS: PARTIALLY IMPLEMENTED. The hub SHELL — gated segments, tab row, entry icon,
 * navigation, dev override — is production code now:
 *   - gating: `domain/service/community/CommunityHubService` (liveSegments =
 *     (shipped ∪ devForced) ∩ capabilities, fail closed)
 *   - screen shell + segmented tab row: `presentation/community/CommunityHubScreen.kt`
 *     (including the Contacts muted-tab treatment and the shell previews)
 *   - entry icon + badge: `presentation/community/CommunityTopBarIcon.kt`
 * What REMAINS design spec in this file is the SEGMENT CONTENT that has not shipped yet:
 *
 * ======================================================================================
 * DISCUSSIONS TAB CONTENT — spec for the Discussions wiring
 * ======================================================================================
 * At milestone 11 the hub renders the Discussion channel's thread directly as its body
 * (no tab row while only one segment is live). [DiscussionsTabContent] below is the
 * target composition: the pinned Support reference on top, then the embedded channel
 * thread (`DiscussionsChannelScreenDesign.kt`, `showTopBar = false`). The production
 * shell's placeholder body gets replaced by exactly this composition. Note the pinned
 * Support row currently rendered at shell level in production moves INTO this content
 * when it ships.
 *
 * ======================================================================================
 * SUPPORT — HUB-SIDE REFERENCE (not a segment)
 * ======================================================================================
 * DECISION (unchanged): the in-app Support channel is referenced from BOTH the existing
 * More → Help → Support screen AND from the hub's pinned "Need help?" row — they coexist,
 * both pointing at the same underlying channel. Support is deliberately NOT a segment in
 * the tab row: desktop keeps official support institutionally separate from casual chat
 * (`NavigationTarget.java` puts SUPPORT as a top-level sibling of CHAT), and the hub's
 * aggregate unread badge deliberately excludes Support from its count (see
 * `CommunityHubService.unreadCount` KDoc for the badge-semantics contract, migrated there
 * from the retired `CommunityEntryPointDesign.kt`). [SupportChannelPinnedReference] below
 * is the row's spec; tapping pushes the Support channel as its own screen — the same
 * thread composable as Discussions, parameterized (see `DiscussionsChannelScreenDesign.kt`
 * "REUSED FOR THE SUPPORT CHANNEL").
 *
 * The previews render the milestone-11 main-screen composition (what the icon opens once
 * the Discussions wiring ships): TopBar, pinned Support reference, embedded thread.
 */
package network.bisq.mobile.presentation.design.community

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.QuestionIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ============================================================================================
// Content — DISCUSSIONS TAB (the milestone-11 hub body, not yet implemented)
// ============================================================================================

/**
 * The Discussions segment's content: pinned Support reference, then the embedded channel
 * thread. This is the composition that replaces the production shell's placeholder body
 * when the Discussions wiring ships. If bisq2 ever ships a second live Discussion channel,
 * this is the reinsertion point for a channel picker (see
 * `DiscussionsChannelScreenDesign.kt` "REINTRODUCING A CHANNEL PICKER").
 */
@Composable
internal fun DiscussionsTabContent(
    uiState: DiscussionsChannelUiState,
    onAction: (DiscussionsChannelUiAction) -> Unit,
    onOpenSupportChannel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SupportChannelPinnedReference(onClick = onOpenSupportChannel)
        DiscussionsChannelScreenContent(
            uiState = uiState,
            onAction = onAction,
            showTopBar = false,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// Support channel pinned reference — see file KDoc "SUPPORT — HUB-SIDE REFERENCE"
// ---------------------------------------------------------------------------

@Composable
private fun SupportChannelPinnedReference(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(BisqTheme.colors.dark_grey40)
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(BisqUIConstants.ScreenPadding3X).background(BisqTheme.colors.dark_grey50, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            QuestionIcon(modifier = Modifier.size(BisqUIConstants.ScreenPadding2X))
        }
        Column(modifier = Modifier.weight(1f)) {
            BisqText.SmallMedium(text = "Need help?", color = BisqTheme.colors.white)
            BisqText.XSmallLight(text = "Visit the Support channel", color = BisqTheme.colors.mid_grey20)
        }
        ArrowRightIcon()
    }
}

// ============================================================================================
// Preview fixtures
// ============================================================================================

private fun discussionsPreviewState() = DiscussionsChannelUiState(channelName = "Discussion", memberCount = 1284, messages = simulatedMessages())

// ============================================================================================
// Previews — the milestone-11 main-screen composition once the Discussions wiring ships
// (TopBar + pinned Support reference + embedded thread; no tab row with one live segment)
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "Hub body target — Discussions + Support reference, Populated")
@Composable
private fun DiscussionsTabContent_PopulatedPreview() {
    BisqTheme.Preview {
        Column {
            TopBarContent(title = "Community", showBackButton = true, showUserAvatar = true)
            DiscussionsTabContent(
                uiState = discussionsPreviewState(),
                onAction = {},
                onOpenSupportChannel = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Hub body target — Loading")
@Composable
private fun DiscussionsTabContent_LoadingPreview() {
    BisqTheme.Preview {
        Column {
            TopBarContent(title = "Community", showBackButton = true, showUserAvatar = true)
            DiscussionsTabContent(
                uiState = DiscussionsChannelUiState(channelName = "Discussion", memberCount = 1284, isLoading = true),
                onAction = {},
                onOpenSupportChannel = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Hub body target — Empty (no messages yet)")
@Composable
private fun DiscussionsTabContent_EmptyPreview() {
    BisqTheme.Preview {
        Column {
            TopBarContent(title = "Community", showBackButton = true, showUserAvatar = true)
            DiscussionsTabContent(
                uiState = DiscussionsChannelUiState(channelName = "Discussion", memberCount = 1284, messages = emptyList()),
                onAction = {},
                onOpenSupportChannel = {},
            )
        }
    }
}

/**
 * Interactive preview: tapping the inline search icon (in the member-count row, since this
 * embedded context has no TopBar of its own) reveals the search-in-channel field. Also
 * demonstrates the pinned Support reference row is present and tappable.
 */
@ExcludeFromCoverage
@Preview(name = "Hub body target — Interactive (search toggle + Support reference)")
@Composable
private fun DiscussionsTabContent_InteractivePreview() {
    var searchActive by remember { mutableStateOf(false) }
    var supportTapped by remember { mutableStateOf(false) }
    BisqTheme.Preview {
        Column {
            TopBarContent(title = "Community", showBackButton = true, showUserAvatar = true)
            if (supportTapped) {
                BisqText.SmallRegular(
                    text = "→ OnOpenSupportChannel fired (would push the Support channel screen)",
                    color = BisqTheme.colors.primary,
                    modifier = Modifier.padding(BisqUIConstants.ScreenPaddingHalf),
                )
            }
            DiscussionsTabContent(
                uiState =
                    DiscussionsChannelUiState(
                        channelName = "Discussion",
                        memberCount = 1284,
                        messages = simulatedMessages(),
                        isSearchActive = searchActive,
                    ),
                onAction = { action ->
                    if (action is DiscussionsChannelUiAction.OnToggleSearch) searchActive = !searchActive
                },
                onOpenSupportChannel = { supportTapped = true },
            )
        }
    }
}
