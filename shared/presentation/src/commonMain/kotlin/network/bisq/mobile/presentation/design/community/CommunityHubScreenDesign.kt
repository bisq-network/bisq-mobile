/**
 * CommunityHubScreenDesign.kt — Design PoC (Milestone 11 "Bisq community", issue #589)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * The full screen pushed when the user taps `CommunityTopBarIcon` (see
 * CommunityEntryPointDesign.kt). This IS the canonical Community hub design — a 3-tab
 * segmented shell: **Discussions | Messages | Contacts**. There is no separate
 * "future" version of this screen; this is the target design at every milestone,
 * including milestone 11.
 *
 * ======================================================================================
 * CANONICAL DESIGN, GATED ROLLOUT (consolidation, rodvar's 3rd review pass)
 * ======================================================================================
 * Earlier drafts of this file shipped a Discussions-only screen for milestone 11 PLUS a
 * separately-named "future segmented hub" preview showing the eventual 3-tab shape.
 * rodvar had those consolidated: nothing is built yet, so there is no repaint-twice cost
 * to designing the full target now, and maintaining two hub designs invited drift
 * between them. There is now exactly ONE hub design, `CommunityHubScreenContent`,
 * structured as the full 3-tab shell from the start — not a Discussions screen that
 * later grows tabs.
 *
 * THE ROLLOUT IS GATED, NOT THE DESIGN. `CommunityHubUiState.liveSegments` is the set of
 * segments whose backing feature actually exists yet. `CommunitySegmentedTabRow` renders
 * ONLY the tabs present in `liveSegments` — a segment whose feature hasn't shipped is
 * NEVER rendered as a disabled/greyed/"coming soon" tab; it simply isn't in the row.
 * Further, when `liveSegments.size <= 1` the tab row doesn't render AT ALL — a segmented
 * control with a single segment has nothing to switch between, so showing one would be
 * pure visual noise, not a real affordance. In that state Discussions renders directly
 * with no tab chrome at all, which is exactly what a milestone-11 user sees.
 *
 * Milestone-by-milestone `liveSegments` (same composable, same code, different state):
 *   - **Milestone 11 (now)**: `{DISCUSSIONS}`. #589 ships; #590/#1238 don't exist yet.
 *     Tab row is invisible; the user sees a plain Discussions screen. This is the
 *     REALISTIC shipping state — see the `CommunityHubScreen_Milestone11_*` previews.
 *   - **#590 ships**: `{DISCUSSIONS, MESSAGES}`. Tab row appears with exactly 2 tabs;
 *     Contacts is still simply absent, not a disabled 3rd tab. See
 *     `CommunityHubScreen_TwoSegmentsLivePreview`.
 *   - **#1238 ships**: `{DISCUSSIONS, MESSAGES, CONTACTS}`. The full target design. See
 *     `CommunityHubScreen_AllSegmentsLiveInteractivePreview`.
 * No caller changes the composable's shape between these states — only what's in
 * `liveSegments` changes, which is exactly the point of designing the target now.
 *
 * ======================================================================================
 * SEGMENT MAPPING (decided)
 * ======================================================================================
 *   - **Discussions** = public multi-party channels, #589. `DiscussionsTabContent`
 *     below (search + section-grouped channel list) is the tab's body. This used to be
 *     this file's entire standalone screen; it is now purely the Discussions tab's
 *     content, with its own TopBar removed — the shell owns ONE shared TopBar for all
 *     tabs (see LAYOUT below).
 *   - **Messages** = the private-DM inbox, #590. `PrivateChatListScreenContent`
 *     (design/community/private_chat/PrivateChatListScreenDesign.kt) reused directly,
 *     `showTopBar = false` since the shell owns the TopBar. See that file's "ROLE IN THE
 *     HUB" section for why DMs and public channels stay in separate segments rather than
 *     one merged list (they are distinct objects: a DM is a persistent 1:1 relationship
 *     you can leave; a channel is implicit, non-exclusive membership with no "leave").
 *   - **Contacts** = the relationship directory, #1238, NOT designed yet — deliberately
 *     not a message list, see project_milestone11_community_ia.md agent memory.
 *     `ContactsDirectoryPlaceholder` below is a labelled stand-in only.
 *
 * ======================================================================================
 * WHY DISCUSSIONS + SUPPORT SHARE ONE LIST WITHIN THE DISCUSSIONS TAB (NOT TWO TABS)
 * ======================================================================================
 * Desktop treats "Discussions" and "Support" as two instances of the same reusable
 * channel-tab shape (bisq2/.../desktop/main/content/chat/common/pub/ — CommonChatTabView
 * driven by a channel "kind"). On mobile there is no room for a second layer of tabs
 * inside the Discussions tab, so channels are grouped with section headers in a single
 * scrollable list instead, tagged via `SimulatedChannelCategory`. This keeps the two
 * categories visually distinct (a user browsing casual discussion doesn't want
 * support-ticket channels interleaved) without adding another navigation layer on top
 * of the hub's own 3-tab layer.
 *
 * ======================================================================================
 * OPEN QUESTION — NOT RESOLVED HERE (per explicit instruction, left as a question)
 * ======================================================================================
 * The existing More → Help → Support entry (MiscItemsPresenter.buildSections, `help`
 * section) may need to be reconciled with the new Support channel category shown here —
 * does the old entry redirect into the Discussions tab's Support section, or do both
 * coexist? Do NOT remove the existing More-menu Support entry as part of this design
 * pass; this screen assumes the community Support channel simply exists alongside it
 * for now. Decide when implementing.
 *
 * ======================================================================================
 * SEARCH (#589 "reuses ... a search component")
 * ======================================================================================
 * This is the DIRECTORY-level search inside the Discussions tab — filters the channel
 * list by name/description. It is a DIFFERENT search from the per-channel message
 * search inside a channel thread (see DiscussionsChannelScreenDesign.kt's
 * search-in-channel toggle). Reuses the real production `BisqSearchField` molecule
 * directly (common/ui/components/molecules/inputfield/SearchField.kt) — it only takes
 * primitives, so no Simulated stand-in is needed here.
 *
 * ======================================================================================
 * LAYOUT
 * ======================================================================================
 * Milestone 11 (liveSegments = {DISCUSSIONS} — tab row hidden):
 * ┌─────────────────────────────────────────┐
 * │ TopBar: "← Community"                    │
 * ├─────────────────────────────────────────┤
 * │ [ Search channels...            🔍 ]     │
 * ├─────────────────────────────────────────┤
 * │ DISCUSSION                               │  section label
 * │  💬 General Discussion      3 unread     │
 * │  💬 Market Chat                          │
 * ├─────────────────────────────────────────┤
 * │ SUPPORT                                  │  section label
 * │  ❓ New Trader Help          1 unread     │
 * └─────────────────────────────────────────┘
 *
 * Once a 2nd/3rd segment is live (tab row appears — same TopBar, same tab bodies):
 * ┌─────────────────────────────────────────┐
 * │ TopBar: "← Community"                    │
 * ├─────────────────────────────────────────┤
 * │  Discussions ⑤ │  Messages ③ │ Contacts  │  ← CommunitySegmentedTabRow
 * ├─────────────────────────────────────────┤
 * │            (selected tab's body)         │
 * └─────────────────────────────────────────┘
 *
 * Section-label styling reuses the exact decision made for issue #1520
 * (see agent memory: project_more_menu_redesign.md) — mid_grey20 + XSmallMedium + all
 * caps, chosen over small-caps because IBM Plex Sans has no small-caps axis and OpenType
 * "smcp" is unreliable on Android.
 *
 * ======================================================================================
 * i18n KEYS NEEDED
 * ======================================================================================
 * mobile.community.hub.title              → "Community"
 * mobile.community.hub.search.hint        → "Search channels..."
 * mobile.community.hub.section.discussion → "Discussion"
 * mobile.community.hub.section.support    → "Support"
 * mobile.community.hub.empty              → "No channels available"
 * mobile.community.hub.search.noResults   → "No channels match \"{0}\""
 * mobile.community.hub.memberCount        → "{0} members"
 * mobile.community.hub.tab.discussions    → "Discussions" (CommunitySegmentedTabRow
 *   currently derives this from the enum name in English as a PoC placeholder — needs a
 *   real key at implementation time)
 * mobile.community.hub.tab.messages       → "Messages"
 * mobile.community.hub.tab.contacts       → "Contacts"
 *
 * ======================================================================================
 * TEXT EXPANSION
 * ======================================================================================
 * Channel names are operator-authored short labels (2-3 words typically) — low risk.
 * "members" count suffix in German ("Mitglieder") is noticeably longer; the member-count
 * text uses SmallLight sized text with no fixed-width container, so it wraps naturally
 * rather than truncating. Tab labels ("Discussions"/"Messages"/"Contacts") are short in
 * every supported language — low risk for the segmented row.
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ChatIconOutlined
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.QuestionIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
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

internal enum class SimulatedChannelCategory {
    DISCUSSION,
    SUPPORT,
}

internal data class SimulatedChannel(
    val id: String,
    val name: String,
    val category: SimulatedChannelCategory,
    val lastMessagePreview: String,
    val lastMessageTime: String,
    val unreadCount: Int,
    val memberCount: Int,
)

internal enum class SimulatedHubSegment {
    DISCUSSIONS,
    MESSAGES,
    CONTACTS,
}

// ============================================================================================
// UiState / UiAction — SHELL (the 3-tab hub itself)
// ============================================================================================

/**
 * The shell's own state. Composes the two sub-screens' state types directly
 * (`DiscussionsTabUiState`, `PrivateChatListUiState`) rather than duplicating their
 * fields — this screen owns tab selection and gating, not the tabs' own data.
 *
 * @param liveSegments which tabs actually have a shipped feature behind them — see file
 *   KDoc "CANONICAL DESIGN, GATED ROLLOUT". Defaults to milestone 11's real state.
 */
internal data class CommunityHubUiState(
    val selectedSegment: SimulatedHubSegment = SimulatedHubSegment.DISCUSSIONS,
    val liveSegments: Set<SimulatedHubSegment> = setOf(SimulatedHubSegment.DISCUSSIONS),
    val discussions: DiscussionsTabUiState = DiscussionsTabUiState(),
    val messages: PrivateChatListUiState = PrivateChatListUiState(),
)

internal sealed interface CommunityHubUiAction {
    data class OnSegmentSelect(
        val segment: SimulatedHubSegment,
    ) : CommunityHubUiAction

    data class OnDiscussionsAction(
        val action: DiscussionsTabUiAction,
    ) : CommunityHubUiAction

    data class OnMessagesAction(
        val action: PrivateChatListUiAction,
    ) : CommunityHubUiAction
}

// ============================================================================================
// UiState / UiAction — DISCUSSIONS TAB (formerly this file's entire standalone screen)
// ============================================================================================

internal data class DiscussionsTabUiState(
    val channels: List<SimulatedChannel> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
)

internal sealed interface DiscussionsTabUiAction {
    data class OnSearchQueryChange(
        val query: String,
    ) : DiscussionsTabUiAction

    data class OnChannelClick(
        val channelId: String,
    ) : DiscussionsTabUiAction
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

        // See file KDoc "CANONICAL DESIGN, GATED ROLLOUT": the tab row only renders once
        // there is more than one live segment to switch between. A single-segment row
        // would be a control with nothing to control.
        if (uiState.liveSegments.size > 1) {
            CommunitySegmentedTabRow(
                selected = effectiveSegment,
                onSelect = { onAction(CommunityHubUiAction.OnSegmentSelect(it)) },
                liveSegments = uiState.liveSegments,
                counts =
                    mapOf(
                        SimulatedHubSegment.DISCUSSIONS to uiState.discussions.channels.sumOf { it.unreadCount },
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
 *   "CANONICAL DESIGN, GATED ROLLOUT". A segment absent from this set is never shown as
 *   a disabled/greyed tab, it is simply not in the row.
 * @param counts renders a small unread pill next to a segment's label when its count is
 *   > 0, matching the same manual-`Box`-pill style used by `ChannelRow`/`ConversationRow`
 *   (not `BadgedBox` — see the badge-clipping bug documented in
 *   `CommunityEntryPointDesign.kt` for why that pattern is avoided here too).
 */
@Composable
internal fun CommunitySegmentedTabRow(
    selected: SimulatedHubSegment,
    onSelect: (SimulatedHubSegment) -> Unit,
    liveSegments: Set<SimulatedHubSegment> = SimulatedHubSegment.entries.toSet(),
    counts: Map<SimulatedHubSegment, Int> = emptyMap(),
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = BisqUIConstants.ScreenPadding)) {
        SimulatedHubSegment.entries.filter { it in liveSegments }.forEach { segment ->
            val isSelected = segment == selected
            val count = counts[segment] ?: 0
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
                        color = if (isSelected) BisqTheme.colors.primary else BisqTheme.colors.mid_grey20,
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
                            .background(if (isSelected) BisqTheme.colors.primary else BisqTheme.colors.dark_grey50),
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
// Content — DISCUSSIONS TAB (search + section-grouped channel list, no TopBar of its own)
// ============================================================================================

@Composable
internal fun DiscussionsTabContent(
    uiState: DiscussionsTabUiState,
    onAction: (DiscussionsTabUiAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding)) {
            BisqSearchField(
                value = uiState.searchQuery,
                onValueChange = { onAction(DiscussionsTabUiAction.OnSearchQueryChange(it)) },
                placeholder = "Search channels...",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when {
            uiState.isLoading -> DiscussionsTabLoadingState()
            uiState.channels.isEmpty() && uiState.searchQuery.isEmpty() -> DiscussionsTabEmptyState()
            uiState.channels.isEmpty() -> DiscussionsTabNoSearchResultsState(uiState.searchQuery)
            else -> {
                val discussionChannels = uiState.channels.filter { it.category == SimulatedChannelCategory.DISCUSSION }
                val supportChannels = uiState.channels.filter { it.category == SimulatedChannelCategory.SUPPORT }

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (discussionChannels.isNotEmpty()) {
                        ChannelSectionLabel("Discussion")
                        discussionChannels.forEach { channel ->
                            ChannelRow(channel = channel, onClick = { onAction(DiscussionsTabUiAction.OnChannelClick(channel.id)) })
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = BisqTheme.colors.dark_grey50,
                                modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
                            )
                        }
                    }
                    if (supportChannels.isNotEmpty()) {
                        ChannelSectionLabel("Support")
                        supportChannels.forEach { channel ->
                            ChannelRow(channel = channel, onClick = { onAction(DiscussionsTabUiAction.OnChannelClick(channel.id)) })
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
}

// ---------------------------------------------------------------------------
// Section label — mirrors the #1520 More-menu section-header decision
// ---------------------------------------------------------------------------

@Composable
private fun ChannelSectionLabel(text: String) {
    BisqText.XSmallMedium(
        text = text.uppercase(),
        color = BisqTheme.colors.mid_grey20,
        modifier =
            Modifier.padding(
                start = BisqUIConstants.ScreenPadding,
                top = BisqUIConstants.ScreenPadding,
                bottom = BisqUIConstants.ScreenPaddingHalf,
            ),
    )
}

// ---------------------------------------------------------------------------
// Channel row
// ---------------------------------------------------------------------------

@Composable
private fun ChannelRow(
    channel: SimulatedChannel,
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
            if (channel.category == SimulatedChannelCategory.SUPPORT) {
                QuestionIcon(modifier = Modifier.size(BisqUIConstants.ScreenPadding2X))
            } else {
                ChatIconOutlined(modifier = Modifier.size(BisqUIConstants.ScreenPadding2X))
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BisqText.BaseRegular(text = channel.name, color = BisqTheme.colors.white, singleLine = true, modifier = Modifier.weight(1f))
                BisqText.SmallRegular(text = channel.lastMessageTime, color = BisqTheme.colors.mid_grey20)
            }
            BisqText.SmallLight(text = channel.lastMessagePreview, color = BisqTheme.colors.mid_grey30)
            BisqText.SmallLight(text = "${channel.memberCount} members", color = BisqTheme.colors.mid_grey20)
        }

        if (channel.unreadCount > 0) {
            Box(
                modifier =
                    Modifier
                        .background(BisqTheme.colors.primary, shape = CircleShape)
                        .padding(horizontal = BisqUIConstants.ScreenPaddingHalf, vertical = BisqUIConstants.ScreenPaddingQuarter),
            ) {
                BisqText.XSmallMedium(text = channel.unreadCount.toString(), color = BisqTheme.colors.white)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Loading / empty / no-results states
// ---------------------------------------------------------------------------

@Composable
private fun DiscussionsTabLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BisqTheme.colors.primary, strokeWidth = 2.dp)
    }
}

@Composable
private fun DiscussionsTabEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(BisqUIConstants.ScreenPadding2X),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.BaseLight(
            text = "No channels available",
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DiscussionsTabNoSearchResultsState(query: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(BisqUIConstants.ScreenPadding2X),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.BaseLight(
            text = "No channels match \"$query\"",
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

// ============================================================================================
// Preview fixtures
// ============================================================================================

private fun simulatedChannels() =
    listOf(
        SimulatedChannel(
            id = "general",
            name = "General Discussion",
            category = SimulatedChannelCategory.DISCUSSION,
            lastMessagePreview = "Anyone had luck with SEPA transfers over 500 EUR?",
            lastMessageTime = "2 min",
            unreadCount = 3,
            memberCount = 1284,
        ),
        SimulatedChannel(
            id = "market-chat",
            name = "Market Chat",
            category = SimulatedChannelCategory.DISCUSSION,
            lastMessagePreview = "BTC volatility is wild today",
            lastMessageTime = "1 h",
            unreadCount = 0,
            memberCount = 812,
        ),
        SimulatedChannel(
            id = "new-trader-help",
            name = "New Trader Help",
            category = SimulatedChannelCategory.SUPPORT,
            lastMessagePreview = "How do I increase my reputation score?",
            lastMessageTime = "5 min",
            unreadCount = 1,
            memberCount = 456,
        ),
        SimulatedChannel(
            id = "app-support",
            name = "App Support",
            category = SimulatedChannelCategory.SUPPORT,
            lastMessagePreview = "You: Thanks, that fixed it!",
            lastMessageTime = "Yesterday",
            unreadCount = 0,
            memberCount = 301,
        ),
    )

// ============================================================================================
// Previews — MILESTONE 11 realistic state: liveSegments = {DISCUSSIONS}, tab row hidden
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "Community Hub — Milestone 11 (Discussions only, tab row hidden), Populated")
@Composable
private fun CommunityHubScreen_Milestone11_PopulatedPreview() {
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState = CommunityHubUiState(discussions = DiscussionsTabUiState(channels = simulatedChannels())),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Community Hub — Milestone 11, Loading")
@Composable
private fun CommunityHubScreen_Milestone11_LoadingPreview() {
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState = CommunityHubUiState(discussions = DiscussionsTabUiState(isLoading = true)),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Community Hub — Milestone 11, Empty (no channels at all)")
@Composable
private fun CommunityHubScreen_Milestone11_EmptyPreview() {
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState = CommunityHubUiState(discussions = DiscussionsTabUiState(channels = emptyList())),
            onAction = {},
        )
    }
}

/**
 * Interactive preview: typing in the search field filters the channel list live,
 * demonstrating both the "results" and "no results" states of the directory-level
 * search in one preview rather than two separate static ones.
 */
@ExcludeFromCoverage
@Preview(name = "Community Hub — Milestone 11, Interactive search")
@Composable
private fun CommunityHubScreen_Milestone11_InteractiveSearchPreview() {
    var query by remember { mutableStateOf("") }
    val all = simulatedChannels()
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState =
                CommunityHubUiState(
                    discussions =
                        DiscussionsTabUiState(
                            channels = if (query.isEmpty()) all else all.filter { it.name.contains(query, ignoreCase = true) },
                            searchQuery = query,
                        ),
                ),
            onAction = { action ->
                if (action is CommunityHubUiAction.OnDiscussionsAction &&
                    action.action is DiscussionsTabUiAction.OnSearchQueryChange
                ) {
                    query = action.action.query
                }
            },
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Channel row — Discussion, unread")
@Composable
private fun ChannelRow_Discussion_UnreadPreview() {
    BisqTheme.Preview {
        ChannelRow(channel = simulatedChannels()[0], onClick = {})
    }
}

@ExcludeFromCoverage
@Preview(name = "Channel row — Support, no unread")
@Composable
private fun ChannelRow_Support_NoUnreadPreview() {
    BisqTheme.Preview {
        ChannelRow(channel = simulatedChannels()[3], onClick = {})
    }
}

// ============================================================================================
// Previews — ROLLOUT PROGRESSION: 2 segments live, then all 3 (the canonical target)
// ============================================================================================

/**
 * Preview: `liveSegments = {DISCUSSIONS, MESSAGES}` — the state right after #590 ships,
 * Contacts still absent. Proves the tab row shows exactly 2 tabs, not 3-with-one-greyed.
 */
@ExcludeFromCoverage
@Preview(name = "Community Hub — 2 segments live (post-#590, pre-#1238)")
@Composable
private fun CommunityHubScreen_TwoSegmentsLivePreview() {
    var tab by remember { mutableStateOf(SimulatedHubSegment.MESSAGES) }
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState =
                CommunityHubUiState(
                    selectedSegment = tab,
                    liveSegments = setOf(SimulatedHubSegment.DISCUSSIONS, SimulatedHubSegment.MESSAGES),
                    discussions = DiscussionsTabUiState(channels = simulatedChannels()),
                    messages = PrivateChatListUiState(conversations = simulatedConversations()),
                ),
            onAction = { action -> if (action is CommunityHubUiAction.OnSegmentSelect) tab = action.segment },
        )
    }
}

/**
 * Preview: `liveSegments` = all 3 — the full canonical target design, once #1238 ships
 * too. Interactive tab switching between the real Discussions content, the real Messages
 * (private-chat inbox) content, and the Contacts placeholder. This replaces the old,
 * separately-named "future segmented hub" preview — it is now just another state of the
 * SAME canonical `CommunityHubScreenContent`, not a distinct screen.
 */
@ExcludeFromCoverage
@Preview(name = "Community Hub — All 3 segments live (canonical target design)")
@Composable
private fun CommunityHubScreen_AllSegmentsLiveInteractivePreview() {
    var tab by remember { mutableStateOf(SimulatedHubSegment.MESSAGES) }
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState =
                CommunityHubUiState(
                    selectedSegment = tab,
                    liveSegments = SimulatedHubSegment.entries.toSet(),
                    discussions = DiscussionsTabUiState(channels = simulatedChannels()),
                    messages = PrivateChatListUiState(conversations = simulatedConversations()),
                ),
            onAction = { action -> if (action is CommunityHubUiAction.OnSegmentSelect) tab = action.segment },
        )
    }
}
