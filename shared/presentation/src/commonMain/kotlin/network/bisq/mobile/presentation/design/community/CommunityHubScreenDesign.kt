/**
 * CommunityHubScreenDesign.kt — Design PoC (Milestone 11 "Bisq community", issue #589)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * WHAT OPENS WHEN THE USER TAPS THE ICON (one-glance answer)
 * ======================================================================================
 * Tapping the Community top-bar icon (`CommunityEntryPointDesign.kt`) ALWAYS pushes THIS
 * screen, `CommunityHubScreenContent` — there is no other destination and no intermediate
 * screen. What renders INSIDE it depends entirely on `liveSegments`:
 *   - **RIGHT NOW (milestone 11, `liveSegments = {DISCUSSIONS}`)**: the user lands
 *     DIRECTLY on the Discussions content — a shared TopBar reading "Community", then the
 *     pinned "Need help?" Support row, then the Discussion channel's own message thread.
 *     NO tab row, NO channel list, NO picker of any kind in front of it. This is the
 *     preview named **"Community Hub — MAIN SCREEN on icon tap (ships milestone 11)..."**
 *     below — THAT preview, not the 2-or-3-segment ones further down, is what the user
 *     actually sees today when they tap the icon.
 *   - **LATER, as #590/#1238 ship**: this SAME screen grows a segmented tab row
 *     (Discussions | Messages | Contacts) above that same content — see the previews
 *     named **"Community Hub — FUTURE: ..."** further down in this file. Those are NOT
 *     what ships now; don't mistake them for the current main screen.
 *
 * ======================================================================================
 * WHAT SHIPS THIS MILESTONE (detail)
 * ======================================================================================
 * Milestone 11's `liveSegments = {DISCUSSIONS}` means the segmented tab row does not
 * render at all (see "GATED ROLLOUT" below), and the Discussions tab body IS the whole
 * screen: pinned Support reference, then straight into the single Discussion channel's
 * message thread (`DiscussionsChannelScreenDesign.kt`, embedded via `showTopBar = false`).
 * There is no channel list and no search-across-channels — bisq2 wires exactly one
 * Discussion channel and one Support channel today
 * (`chat/src/main/java/bisq/chat/ChatService.java`), so there is nothing to pick between.
 * See `DiscussionsChannelScreenDesign.kt`'s "WHY THERE'S NO CHANNEL LIST" for the
 * evidence (bisq2's own channel-consolidation history) and "REINTRODUCING A CHANNEL
 * PICKER" for the explicit, code-level trigger to revisit this.
 *
 * The 3-segment shell (Discussions | Messages | Contacts) described below is still the
 * eventual target once #590/#1238 ship — it just isn't visible yet, by design, not by
 * omission. Don't let the shell's generality below read as "this is what ships now"; the
 * section above is what ships now.
 *
 * ======================================================================================
 * GATED ROLLOUT
 * ======================================================================================
 * `CommunityHubUiState.liveSegments` is the set of segments whose backing feature
 * actually exists. `CommunitySegmentedTabRow` renders ONLY the tabs present in
 * `liveSegments` — a segment whose feature hasn't shipped is NEVER rendered as a
 * disabled/greyed/"coming soon" tab; it simply isn't in the row. When
 * `liveSegments.size <= 1` the tab row doesn't render AT ALL, since a segmented control
 * with one segment has nothing to switch between.
 *
 * Milestone-by-milestone `liveSegments` (same composable, same code, different state):
 *   - **Milestone 11 (now)**: `{DISCUSSIONS}`. Tab row invisible — see lede above.
 *     `CommunityHubScreen_Milestone11_*` previews.
 *   - **#590 ships**: `{DISCUSSIONS, MESSAGES}`. Tab row appears with exactly 2 tabs.
 *     `CommunityHubScreen_TwoSegmentsLivePreview`.
 *   - **#1238 ships**: `{DISCUSSIONS, MESSAGES, CONTACTS}`. Full target shape.
 *     `CommunityHubScreen_AllSegmentsLiveInteractivePreview`.
 *
 * ======================================================================================
 * SEGMENT MAPPING
 * ======================================================================================
 *   - **Discussions** = the single public Discussion channel, #589. `DiscussionsTabContent`
 *     is the tab's body: the pinned Support reference (see below) followed directly by
 *     `DiscussionsChannelScreenContent` (`DiscussionsChannelScreenDesign.kt`,
 *     `showTopBar = false` since this shell owns the shared TopBar).
 *   - **Messages** = the private-DM inbox, #590. `PrivateChatListScreenContent`
 *     (design/community/private_chat/PrivateChatListScreenDesign.kt) reused directly,
 *     `showTopBar = false`. See that file's "ROLE IN THE HUB" section for why DMs and the
 *     Discussion channel stay in separate segments rather than one merged list (a DM is a
 *     persistent 1:1 relationship you can leave; a channel is implicit, non-exclusive
 *     membership with no "leave").
 *   - **Contacts** = the relationship directory, #1238, NOT designed yet — deliberately
 *     not a message list, see project_milestone11_community_ia.md agent memory.
 *     Auto-populates from trade history AND private chats (bisq2 `ContactReason`:
 *     PRIVATE_CHAT, BISQ_EASY_TRADE, MUSIG_TRADE, MANUALLY_ADDED) plus manual add from
 *     Peer Profile — not trades alone.
 *     `ContactsDirectoryPlaceholder` below is a labelled stand-in only. Rendered with a
 *     MUTED visual treatment even once live — see "CONTACTS — MUTED SEGMENT TREATMENT".
 *
 * ======================================================================================
 * SUPPORT — HUB-SIDE REFERENCE (not a segment)
 * ======================================================================================
 * DECISION: the in-app Support public channel is referenced from BOTH the existing
 * More → Help → Support screen (`SupportScreen.kt`, today a static list of external
 * links — Matrix, forum) AND from here, in the Community hub. They coexist rather than
 * one replacing the other, because support is critical in a fully decentralized app that
 * many users find hard to reason about — a single, easy-to-miss entry point isn't good
 * enough for something this important. This resolves the previously-deferred "does the
 * new Support channel replace or coexist with More → Help → Support" question:
 * COEXIST, both pointing at the same underlying channel.
 *
 * What Support is NOT, deliberately: a 4th co-equal segment in `CommunitySegmentedTabRow`.
 * Reasons (design-review finding, not a style preference):
 *   1. Desktop precedent contradicts tab-parity: `NavigationTarget.java` places `SUPPORT`
 *      as its OWN top-level `CONTENT` section, a sibling of `CHAT` (not nested inside it)
 *      alongside `SUPPORT_ASSISTANCE` (the same channel, reached via desktop's Help area)
 *      and `SUPPORT_RESOURCES` (static help content). Desktop keeps "official support"
 *      institutionally separate from casual Discussion chat, at the same rank as Academy
 *      or User settings — not as a sibling browsing tab.
 *   2. Badge-math complexity: the hub's aggregate unread badge
 *      (`CommunityEntryPointDesign.kt` "BADGE SEMANTICS") deliberately sums only
 *      Discussions + Messages. Adding Support as a THIRD countable, badge-carrying tab
 *      reopens an ambiguity question (channel mention vs. DM vs. support reply) that
 *      hasn't been designed for — see `CommunityEntryPointDesign.kt`'s "BADGE SEMANTICS".
 *
 * THE CHOSEN TREATMENT — a pinned reference row, not a tab: `SupportChannelPinnedReference`
 * renders as the first thing in the Discussions tab body, above the channel thread — a
 * "Need help?" row with the Support icon, always visible (not dismissible, not buried in
 * a menu), one tap into the Support channel. Tapping it navigates to the SAME
 * `DiscussionsChannelScreenContent` composable, parameterized for the Support channel
 * (`channelName = "Support"`, `showTopBar = true` since it's now a standalone pushed
 * screen) — see `DiscussionsChannelScreenDesign.kt`'s "REUSED FOR THE SUPPORT CHANNEL".
 * No new screen type, no new tab, no badge-math change — just a persistent, always-visible
 * doorway from the one place users are already looking (Community) into a channel that
 * already exists.
 *
 * IS THIS THE RIGHT LEVEL OF PROMINENCE, GIVEN "SUPPORT IS CRITICAL"? A persistent,
 * unmissable, always-on-screen row reachable in exactly one tap is a deliberately strong
 * treatment for something that isn't a tab — this is not a buried menu item. If real
 * usage later shows people still miss it (e.g. support-channel traffic stays flat while
 * More → Help → Support traffic doesn't), that's the trigger to reconsider promoting it
 * further — but escalate on evidence, not preemptively. Flagged here explicitly per
 * design-review instruction rather than silently building something bigger.
 *
 * ======================================================================================
 * CONTACTS — MUTED SEGMENT TREATMENT (design-review finding)
 * ======================================================================================
 * Contacts stays a segment in `CommunitySegmentedTabRow`, not a FAB (rejected pattern —
 * see agent memory for the full reasoning: a FAB borrows the app's ONE existing FAB
 * convention, `BisqFABAddButton` in `OfferbookScreen.kt`, whose established job is
 * "author new content," which doesn't match Contacts being a reference/browse surface;
 * a FAB would also hide the reputation/trust data this app's whole design philosophy
 * says should be prominent, not tucked in a corner). Desktop precedent supports keeping
 * it a peer destination too: `NavigationTarget.CONTACTS_LIST` is its own top-level
 * `CONTENT` section on desktop, sibling to `CHAT`.
 *
 * What DOES change: Contacts renders with a deliberately MUTED visual weight relative to
 * Discussions/Messages, reflecting that it's a reference surface (no unread state, no
 * live-update reason to poll) rather than an inbox. `CommunitySegmentedTabRow`'s
 * `mutedSegments` param (default `{CONTACTS}`) controls this:
 *   - Selected-state color: muted segments use `BisqTheme.colors.light_grey50` instead of
 *     `BisqTheme.colors.primary` for both the label and the underline indicator — so
 *     tapping into Contacts never reads as "you're in an active inbox" the way Discussions
 *     or Messages does when selected.
 *   - Unselected-state color: unchanged (`mid_grey20`, same as any other unselected tab).
 *   - Badge slot: unchanged — Contacts was never in the `counts` map (only Discussions and
 *     Messages contribute), so it already never shows a pill. This was previously
 *     undocumented as intentional; now explicit.
 *   - Position, tap target size, gating: unchanged.
 * See `CommunityHubScreen_ContactsMutedVsPrimaryComparisonPreview` for both selected
 * states side by side.
 *
 * ======================================================================================
 * SEARCH
 * ======================================================================================
 * There is no directory-level search anymore (see lede — nothing to search across when
 * there's one channel). The only search in Discussions is the search-in-channel toggle,
 * now documented in `DiscussionsChannelScreenDesign.kt`'s "SEARCH-IN-CHANNEL" section.
 *
 * ======================================================================================
 * i18n KEYS NEEDED
 * ======================================================================================
 * mobile.community.hub.title                      → "Community"
 * mobile.community.hub.tab.discussions             → "Discussions" (CommunitySegmentedTabRow
 *   currently derives this from the enum name in English as a PoC placeholder — needs a
 *   real key at implementation time)
 * mobile.community.hub.tab.messages                → "Messages"
 * mobile.community.hub.tab.contacts                → "Contacts"
 * mobile.community.hub.supportReference.title      → "Need help?"
 * mobile.community.hub.supportReference.subtitle   → "Visit the Support channel"
 * (Discussions channel body's own keys — search, empty state, member count — are declared
 * in DiscussionsChannelScreenDesign.kt, not duplicated here.)
 *
 * ======================================================================================
 * TEXT EXPANSION
 * ======================================================================================
 * Tab labels ("Discussions"/"Messages"/"Contacts") are short in every supported language
 * — low risk for the segmented row. "Need help?" / "Visit the Support channel" are short
 * phrases; German ("Brauchst du Hilfe?" / "Zum Support-Kanal") is comparable length.
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.QuestionIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.design.community.private_chat.PrivateChatListScreenContent
import network.bisq.mobile.presentation.design.community.private_chat.PrivateChatListUiAction
import network.bisq.mobile.presentation.design.community.private_chat.PrivateChatListUiState
import network.bisq.mobile.presentation.design.community.private_chat.simulatedConversations

// ============================================================================================
// Simulated data — no domain type dependencies
// ============================================================================================

internal enum class SimulatedHubSegment {
    DISCUSSIONS,
    MESSAGES,
    CONTACTS,
}

// ============================================================================================
// UiState / UiAction — SHELL (the 3-tab hub itself)
// ============================================================================================

/**
 * The shell's own state. Composes the sub-screens' state types directly
 * (`DiscussionsChannelUiState`, `PrivateChatListUiState`) rather than duplicating their
 * fields — this screen owns tab selection and gating, not the tabs' own data.
 *
 * @param liveSegments which tabs actually have a shipped feature behind them — see file
 *   KDoc "GATED ROLLOUT". Defaults to milestone 11's real state.
 * @param discussions the Discussion channel's own content state (messages, search,
 *   loading) — see `DiscussionsChannelScreenDesign.kt`. Has no unread concept of its own
 *   (you're actively viewing it), which is why unread tracking lives separately below.
 * @param discussionsUnreadCount SHELL-level unread count for the Discussion channel —
 *   deliberately not a field on `DiscussionsChannelUiState`, because "how many messages
 *   are unread" is a property of the relationship between the user and the channel
 *   (last-read timestamp vs. latest message), not a property of the currently-open
 *   channel view itself. Feeds `CommunitySegmentedTabRow`'s `counts` and, once summed
 *   with Messages, `CommunityTopBarIcon`'s global badge (see
 *   `CommunityEntryPointDesign.kt` "BADGE SEMANTICS").
 */
internal data class CommunityHubUiState(
    val selectedSegment: SimulatedHubSegment = SimulatedHubSegment.DISCUSSIONS,
    val liveSegments: Set<SimulatedHubSegment> = setOf(SimulatedHubSegment.DISCUSSIONS),
    val discussions: DiscussionsChannelUiState = DiscussionsChannelUiState(channelName = "Discussion", memberCount = 0),
    val discussionsUnreadCount: Int = 0,
    val messages: PrivateChatListUiState = PrivateChatListUiState(),
)

internal sealed interface CommunityHubUiAction {
    data class OnSegmentSelect(
        val segment: SimulatedHubSegment,
    ) : CommunityHubUiAction

    data class OnDiscussionsAction(
        val action: DiscussionsChannelUiAction,
    ) : CommunityHubUiAction

    data class OnMessagesAction(
        val action: PrivateChatListUiAction,
    ) : CommunityHubUiAction

    /** Opens the Support channel as its own pushed screen — see "SUPPORT — HUB-SIDE REFERENCE" above. */
    data object OnOpenSupportChannel : CommunityHubUiAction
}

// ============================================================================================
// Content — SHELL
// ============================================================================================

@Composable
internal fun CommunityHubScreenContent(
    uiState: CommunityHubUiState,
    onAction: (CommunityHubUiAction) -> Unit,
) {
    // Defensive fallback: if selectedSegment somehow isn't live (e.g. a segment went
    // live→gone in a future rollback scenario), fall back to the first live segment
    // rather than rendering nothing.
    val effectiveSegment =
        if (uiState.selectedSegment in uiState.liveSegments) uiState.selectedSegment else uiState.liveSegments.first()

    Column(modifier = Modifier.fillMaxSize().background(BisqTheme.colors.backgroundColor)) {
        TopBarContent(title = "Community", showBackButton = true, showUserAvatar = true)

        // See file KDoc "GATED ROLLOUT": the tab row only renders once there is more
        // than one live segment to switch between. A single-segment row would be a
        // control with nothing to control.
        if (uiState.liveSegments.size > 1) {
            CommunitySegmentedTabRow(
                selected = effectiveSegment,
                onSelect = { onAction(CommunityHubUiAction.OnSegmentSelect(it)) },
                liveSegments = uiState.liveSegments,
                counts =
                    mapOf(
                        SimulatedHubSegment.DISCUSSIONS to uiState.discussionsUnreadCount,
                        SimulatedHubSegment.MESSAGES to uiState.messages.conversations.sumOf { it.unreadCount },
                    ),
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (effectiveSegment) {
                SimulatedHubSegment.DISCUSSIONS -> {
                    DiscussionsTabContent(
                        uiState = uiState.discussions,
                        onAction = { onAction(CommunityHubUiAction.OnDiscussionsAction(it)) },
                        onOpenSupportChannel = { onAction(CommunityHubUiAction.OnOpenSupportChannel) },
                    )
                }
                SimulatedHubSegment.MESSAGES -> {
                    PrivateChatListScreenContent(
                        uiState = uiState.messages,
                        onAction = { onAction(CommunityHubUiAction.OnMessagesAction(it)) },
                        showTopBar = false,
                    )
                }
                SimulatedHubSegment.CONTACTS -> ContactsDirectoryPlaceholder()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Segmented tab row — the shell's tab strip, gated by liveSegments
// ---------------------------------------------------------------------------

/**
 * `internal` (not `private`) so `CommunityEntryPointDesign.kt`'s badge drill-down
 * preview can reuse it directly — see that file's KDoc "BADGE SEMANTICS" section for
 * why demonstrating "global badge count == sum of segment counts" matters.
 *
 * @param liveSegments only these segments are rendered as tabs — see file KDoc
 *   "GATED ROLLOUT". A segment absent from this set is never shown as a
 *   disabled/greyed tab, it is simply not in the row.
 * @param counts renders a small unread pill next to a segment's label when its count is
 *   > 0, matching the same manual-`Box`-pill style used by `ConversationRow`
 *   (not `BadgedBox` — see the badge-clipping bug documented in
 *   `CommunityEntryPointDesign.kt` for why that pattern is avoided here too).
 * @param mutedSegments segments that never get the full `primary` selected-state color —
 *   see file KDoc "CONTACTS — MUTED SEGMENT TREATMENT". Defaults to muting Contacts only;
 *   Discussions and Messages always use full `primary` when selected.
 */
@Composable
internal fun CommunitySegmentedTabRow(
    selected: SimulatedHubSegment,
    onSelect: (SimulatedHubSegment) -> Unit,
    liveSegments: Set<SimulatedHubSegment> = SimulatedHubSegment.entries.toSet(),
    counts: Map<SimulatedHubSegment, Int> = emptyMap(),
    mutedSegments: Set<SimulatedHubSegment> = setOf(SimulatedHubSegment.CONTACTS),
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = BisqUIConstants.ScreenPadding)) {
        SimulatedHubSegment.entries.filter { it in liveSegments }.forEach { segment ->
            val isSelected = segment == selected
            val count = counts[segment] ?: 0
            val selectedColor = if (segment in mutedSegments) BisqTheme.colors.light_grey50 else BisqTheme.colors.primary
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(segment) }
                        .padding(vertical = BisqUIConstants.ScreenPaddingHalf),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
                    BisqText.SmallRegular(
                        text = segment.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) selectedColor else BisqTheme.colors.mid_grey20,
                    )
                    if (count > 0) {
                        Box(
                            modifier =
                                Modifier
                                    .background(BisqTheme.colors.primary, shape = CircleShape)
                                    .padding(horizontal = BisqUIConstants.ScreenPaddingHalf, vertical = BisqUIConstants.ScreenPaddingQuarter),
                        ) {
                            BisqText.XSmallMedium(text = count.toString(), color = BisqTheme.colors.white)
                        }
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .padding(top = BisqUIConstants.ScreenPaddingQuarter)
                            .width(BisqUIConstants.ScreenPadding4X)
                            .height(2.dp)
                            .background(if (isSelected) selectedColor else BisqTheme.colors.dark_grey50),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Contacts tab — placeholder only, #1238 not designed yet
// ---------------------------------------------------------------------------

@Composable
private fun ContactsDirectoryPlaceholder() {
    Box(modifier = Modifier.fillMaxSize().padding(BisqUIConstants.ScreenPadding2X), contentAlignment = Alignment.Center) {
        BisqText.BaseLight(
            text = "Contacts directory (#1238) — not designed yet.\nA relationship directory, not a message list.",
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

// ============================================================================================
// Content — DISCUSSIONS TAB (pinned Support reference + the single channel's thread)
// ============================================================================================

/**
 * The Discussions tab's body: a pinned reference into the Support channel above the
 * Discussion channel's own thread. This IS the tab now that there's no channel list — see
 * file KDoc lede and `DiscussionsChannelScreenDesign.kt`'s "WHY THERE'S NO CHANNEL LIST".
 * If bisq2 ever ships a second Discussion channel, this is the reinsertion point for a
 * picker (see that file's "REINTRODUCING A CHANNEL PICKER").
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
// Previews — MAIN SCREEN: what the user actually sees on icon tap TODAY
// (liveSegments = {DISCUSSIONS}, tab row hidden). See file KDoc "WHAT OPENS WHEN THE USER
// TAPS THE ICON" — these come first in the file deliberately.
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "Community Hub — MAIN SCREEN on icon tap (ships milestone 11): Discussions + Support reference, Populated")
@Composable
private fun CommunityHubScreen_Milestone11_PopulatedPreview() {
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState = CommunityHubUiState(discussions = discussionsPreviewState()),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Community Hub — MAIN SCREEN on icon tap (ships milestone 11): Loading")
@Composable
private fun CommunityHubScreen_Milestone11_LoadingPreview() {
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState =
                CommunityHubUiState(
                    discussions = DiscussionsChannelUiState(channelName = "Discussion", memberCount = 1284, isLoading = true),
                ),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Community Hub — MAIN SCREEN on icon tap (ships milestone 11): Empty (no messages yet)")
@Composable
private fun CommunityHubScreen_Milestone11_EmptyPreview() {
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState =
                CommunityHubUiState(
                    discussions = DiscussionsChannelUiState(channelName = "Discussion", memberCount = 1284, messages = emptyList()),
                ),
            onAction = {},
        )
    }
}

/**
 * Interactive preview: tapping the inline search icon (in the member-count row, since
 * this embedded context has no TopBar of its own) reveals the search-in-channel field.
 * Also demonstrates the pinned Support reference row is present and tappable.
 */
@ExcludeFromCoverage
@Preview(name = "Community Hub — MAIN SCREEN on icon tap (ships milestone 11): Interactive (search toggle + Support reference)")
@Composable
private fun CommunityHubScreen_Milestone11_InteractivePreview() {
    var searchActive by remember { mutableStateOf(false) }
    var supportTapped by remember { mutableStateOf(false) }
    BisqTheme.Preview {
        Column {
            if (supportTapped) {
                BisqText.SmallRegular(
                    text = "→ OnOpenSupportChannel fired (would push the Support channel screen)",
                    color = BisqTheme.colors.primary,
                    modifier = Modifier.padding(BisqUIConstants.ScreenPaddingHalf),
                )
            }
            CommunityHubScreenContent(
                uiState =
                    CommunityHubUiState(
                        discussions =
                            DiscussionsChannelUiState(
                                channelName = "Discussion",
                                memberCount = 1284,
                                messages = simulatedMessages(),
                                isSearchActive = searchActive,
                            ),
                    ),
                onAction = { action ->
                    when {
                        action is CommunityHubUiAction.OnDiscussionsAction &&
                            action.action is DiscussionsChannelUiAction.OnToggleSearch -> searchActive = !searchActive
                        action is CommunityHubUiAction.OnOpenSupportChannel -> supportTapped = true
                        else -> {}
                    }
                },
            )
        }
    }
}

// ============================================================================================
// Previews — FUTURE: rollout progression, NOT what ships at milestone 11
// (2 segments live, then all 3 — the eventual target once #590/#1238 ship). Named
// "FUTURE: ..." deliberately so these are never mistaken for the current main screen —
// see file KDoc "WHAT OPENS WHEN THE USER TAPS THE ICON".
// ============================================================================================

/**
 * Preview: `liveSegments = {DISCUSSIONS, MESSAGES}` — the state right after #590 ships,
 * Contacts still absent. Proves the tab row shows exactly 2 tabs, not 3-with-one-greyed.
 * NOT the current main screen — see file KDoc lede.
 */
@ExcludeFromCoverage
@Preview(name = "Community Hub — FUTURE: 2 segments live, after #590 ships (NOT what ships at milestone 11)")
@Composable
private fun CommunityHubScreen_TwoSegmentsLivePreview() {
    var tab by remember { mutableStateOf(SimulatedHubSegment.MESSAGES) }
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState =
                CommunityHubUiState(
                    selectedSegment = tab,
                    liveSegments = setOf(SimulatedHubSegment.DISCUSSIONS, SimulatedHubSegment.MESSAGES),
                    discussions = discussionsPreviewState(),
                    discussionsUnreadCount = 3,
                    messages = PrivateChatListUiState(conversations = simulatedConversations()),
                ),
            onAction = { action -> if (action is CommunityHubUiAction.OnSegmentSelect) tab = action.segment },
        )
    }
}

/**
 * Preview: `liveSegments` = all 3 — the full canonical target design, once #1238 ships
 * too. Interactive tab switching between the real Discussions content, the real Messages
 * (private-chat inbox) content, and the Contacts placeholder (muted tab treatment).
 * NOT the current main screen — see file KDoc lede. rodvar flagged (2026-08-17) that this
 * preview was mistaken for "what the icon opens today"; it is the target AFTER #590 and
 * #1238 both ship, not before.
 */
@ExcludeFromCoverage
@Preview(name = "Community Hub — FUTURE: all 3 segments live, after #590 + #1238 ship (NOT what ships at milestone 11)")
@Composable
private fun CommunityHubScreen_AllSegmentsLiveInteractivePreview() {
    var tab by remember { mutableStateOf(SimulatedHubSegment.MESSAGES) }
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState =
                CommunityHubUiState(
                    selectedSegment = tab,
                    liveSegments = SimulatedHubSegment.entries.toSet(),
                    discussions = discussionsPreviewState(),
                    discussionsUnreadCount = 3,
                    messages = PrivateChatListUiState(conversations = simulatedConversations()),
                ),
            onAction = { action -> if (action is CommunityHubUiAction.OnSegmentSelect) tab = action.segment },
        )
    }
}

// ============================================================================================
// Preview — Contacts muted-tab treatment, side by side with a full-primary tab
// ============================================================================================

/**
 * Preview: proves the "CONTACTS — MUTED SEGMENT TREATMENT" claim — Discussions selected
 * (full `primary` color) directly above Contacts selected (muted `light_grey50`), so the
 * visual weight difference is checkable in one glance without switching tabs.
 */
@ExcludeFromCoverage
@Preview(name = "Community Hub — Contacts muted vs Discussions full-primary (tab row comparison)")
@Composable
private fun CommunityHubScreen_ContactsMutedVsPrimaryComparisonPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            BisqText.SmallRegular("Discussions selected (full primary):", color = BisqTheme.colors.mid_grey20)
            CommunitySegmentedTabRow(
                selected = SimulatedHubSegment.DISCUSSIONS,
                onSelect = {},
                liveSegments = SimulatedHubSegment.entries.toSet(),
                counts = mapOf(SimulatedHubSegment.DISCUSSIONS to 3, SimulatedHubSegment.MESSAGES to 2),
            )
            BisqText.SmallRegular("Contacts selected (muted — no badge slot, deliberately):", color = BisqTheme.colors.mid_grey20)
            CommunitySegmentedTabRow(
                selected = SimulatedHubSegment.CONTACTS,
                onSelect = {},
                liveSegments = SimulatedHubSegment.entries.toSet(),
                counts = mapOf(SimulatedHubSegment.DISCUSSIONS to 3, SimulatedHubSegment.MESSAGES to 2),
            )
        }
    }
}
