/**
 * CommunityEntryPointDesign.kt — Design PoC (Milestone 11 "Bisq community")
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * IA DECISION THIS IMPLEMENTS
 * ======================================================================================
 * Per the milestone-11 IA recommendation (see agent memory:
 * project_milestone11_community_ia.md), the community cluster — public channels (#589),
 * private DMs (#590, fast-follow), Contacts (#1238, fast-follow) — is entered through a
 * single ambient top-bar icon, NOT a 5th bottom-nav tab. Reasoning recap:
 *   - The bottom nav (Home · Offers · My Trades · More, see TabContainerScreen.kt L54-76)
 *     is already at capacity; a 5th tab would crowd the app's core economic loop.
 *   - The TopBar has exactly one free slot (its `extraActions` RowScope slot, rendered
 *     BEFORE the user avatar — see TopBar.kt L69/L121/L160-162), which is used here.
 *   - The icon is persistent across all 4 tabs (TopBar is shared chrome, rendered once
 *     in TabContainerScreen.kt), so it is reachable from anywhere without a tab switch —
 *     this is the "return to an open conversation" ambient path that private DMs need.
 *
 * ======================================================================================
 * COMPONENT UNDER DESIGN: CommunityTopBarIcon
 * ======================================================================================
 * Renders inside TopBar's `extraActions` slot:
 * ```
 * TopBar(
 *     title = ...,
 *     extraActions = { CommunityTopBarIcon(unreadCount, showAnimation, onClick) },
 * )
 * ```
 * This is a global affordance — it should be added to the ONE shared TopBar call site
 * in TabContainerScreen.kt (~L85-98), not per-screen, so it appears on Home/Offers/
 * My Trades/More identically.
 *
 * ======================================================================================
 * ICON CHOICE
 * ======================================================================================
 * Uses `ChatIconOutlined` — an existing production icon atom
 * (common/ui/components/atoms/icons/Icons.kt, backed by drawable/icon_chat_outlined.png).
 * Outline (not filled) mirrors the rationale already established by
 * `OpenPrivateChatIconButton` in design/community/private_chat/OpenPrivateChatButtonDesign.kt: an outline
 * icon reads as an ambient, low-weight affordance rather than a heavy, attention-grabbing
 * one, which matters here because this icon is visible on every screen at all times.
 * It must not visually compete with the user's own profile avatar directly beside it.
 *
 * ======================================================================================
 * BADGE
 * ======================================================================================
 * Reuses the existing `AnimatedBadge` atom (atoms/animations/AnimatedBadge.kt) — the same
 * pulse mechanic already used by the bottom-nav My-Trades unread badge
 * (BottomNavigation.kt, MY_TRADES_TAB_INDEX). Per the IA note, that badge is currently
 * hard-coded to one tab index; generalizing its "count → pill" logic into a shared helper
 * benefits both call sites and is flagged there as follow-up work, not solved here.
 *
 * Count formatting caps at "99+" (`formatUnreadBadgeCount`), consistent with the
 * mainstream-messaging-app convention (WhatsApp/Telegram) of never letting a 3+ digit
 * unread count blow out the badge's fixed pill shape.
 *
 * ----------------------------------------------------------------------------------------
 * BADGE SEMANTICS — what the number means, precisely (review pass, rodvar's #1)
 * ----------------------------------------------------------------------------------------
 * DEFINITION: `unreadCount` is the GLOBAL community unread count — the sum of
 *   (a) Discussions public-channel unread (per bisq2's own per-channel notification
 *       rule, `ChatChannelNotificationTypeEnum` — GLOBAL_DEFAULT/ALL/MENTION/OFF; see
 *       CommunityPushNotificationsDesign.kt for why MENTION is the recommended default
 *       for busy public channels) — SHIPS THIS MILESTONE, and
 *   (b) private-DM unread (#590, folds in once Messages ships) — NOT counted this
 *       milestone because there is nothing to count yet.
 * THIS MILESTONE, `CommunityHubUiState.liveSegments = {DISCUSSIONS}` (see
 * CommunityHubScreenDesign.kt's "CANONICAL DESIGN, GATED ROLLOUT"), so `unreadCount` ==
 * total unread across Discussions channels only — there's nothing else live to sum yet.
 * The badge's math does not change shape when DMs ship — it simply gains a second
 * addend as `liveSegments` grows. No caller code needs to change, only what it sums.
 *
 * "HOW DOES THE USER KNOW THEY HAVE A NEW DM SPECIFICALLY?" — the drill-down path (once
 * Messages is live and its segment tab is visible):
 *   1. GLOBAL badge (this component) increments — tells the user "something in
 *      Community needs attention" from anywhere in the app.
 *   2. Inside the hub, the **Messages segment** of `CommunitySegmentedTabRow`
 *      (CommunityHubScreenDesign.kt) shows its OWN count next to its label — tells the
 *      user "the new thing is a message, not a channel reply", narrowing from "global"
 *      to "which segment".
 *   3. The specific **conversation row** (`ConversationRow` in
 *      PrivateChatListScreenDesign.kt — see `ConversationRow_WithUnreadPreview`) shows
 *      its own unread pill — tells the user exactly WHO it's from.
 * A DM is never invisible: it is always represented at all three levels simultaneously.
 * `CommunityHubScreen_TwoSegmentsLivePreview` and
 * `CommunityHubScreen_AllSegmentsLiveInteractivePreview` (CommunityHubScreenDesign.kt —
 * both states of the SAME canonical hub design, not a separate "future" screen) already
 * pass real per-segment `counts` to `CommunitySegmentedTabRow` for level 2; the
 * `CommunityTopBarIcon_DrillDownPreview` below proves level 1's number is exactly the
 * sum of level 2's numbers, and points to level 3.
 *
 * IS A SINGLE AGGREGATE BADGE AMBIGUOUS (channel mention vs. DM)? Yes, at level 1 alone
 * — a lone "5" on the top-bar icon does not say whether that's 5 channel mentions, 5 DM
 * messages, or a mix. RECOMMENDATION: accept that ambiguity at the global level and rely
 * on the drill-down above to resolve it one tap in, rather than over-building. Rationale:
 * (a) this milestone the ambiguity doesn't exist yet — Discussions is the only source;
 * (b) once DMs ship, the two sources are both "someone wants your attention in
 * Community" — the SAME mainstream apps this design already takes cues from (WhatsApp's
 * app-icon badge, Telegram's) also use one aggregate number at the outermost level and
 * resolve the "what kind" question one screen in, not at the icon itself. If real usage
 * shows this reads as confusing once DMs exist, the two options to consider THEN
 * (not now) are: a colour-coded split badge (e.g. green pill for channels, a distinct
 * accent for DMs) or two small dots instead of one pill. Neither is built here —
 * documented as the fallback path only.
 *
 * ----------------------------------------------------------------------------------------
 * BUG FIX (found by rodvar in review): badge was clipped, count not visible
 * ----------------------------------------------------------------------------------------
 * `AnimatedBadge`'s defaults (`xOffset = 8.dp, yOffset = (-8).dp`) were tuned for the
 * bottom-nav context, where `BottomNavigation.kt` places the badge inside a `Column`
 * that has generous surrounding room (icon + gap + label text below → ~72 dp of total
 * allocated item height). Material3's `TopAppBar` is a fixed ~64 dp `Surface`, which
 * clips its content to its own rectangular bounds. `BadgedBox` already anchors the badge
 * at the icon's top-end corner by default; stacking `AnimatedBadge`'s additional
 * `(+8 dp, -8 dp)` offset on top of that pushed the pill's top edge above the TopAppBar's
 * clip boundary, so the count was cut off — the icon and its positioning inside the
 * `extraActions` slot were correct and unaffected.
 *
 * Fix: override `AnimatedBadge`'s offsets with much smaller values tuned for this
 * compact context (`xOffset = 2.dp, yOffset = (-2).dp`), and give the `BadgedBox` a
 * couple of dp of its own padding as extra breathing room. This keeps the badge fully
 * inside the TopAppBar's bounds at every unread-count width (including "99+") while
 * still reading as a top-right corner badge. `PrivateChatListScreenDesign.kt`'s
 * `ConversationRow` and `CommunityHubScreenDesign.kt`'s `ChannelRow` sidestep this
 * failure mode entirely by using a plain `Box` pill instead of `BadgedBox` — they have
 * no ancestor with a fixed-height clipping `Surface`, but this component does, hence
 * the different treatment here.
 *
 * ======================================================================================
 * NAVIGATION (not implemented here — documented for the implementation pass)
 * ======================================================================================
 * Tapping always PUSHES a full screen (`NavRoute.CommunityHub`, new route to add to
 * NavRoute.kt) — never a bottom sheet or popup. This mirrors Instagram/Twitter's DM-icon
 * behaviour: a persistent global icon that always opens a real, navigable screen, so the
 * destination has room to grow (segmented tabs, search, etc.) without redesigning the
 * entry point later.
 *
 * ======================================================================================
 * i18n KEYS NEEDED
 * ======================================================================================
 * mobile.community.entry.contentDescription → "Community" (accessibility label baseline;
 *   production should also announce the unread count, e.g. via a dynamic a11y label —
 *   not modeled here since this PoC intentionally avoids `semantics { contentDescription }`
 *   in favour of `Modifier.testTag(...)` per this project's PoC convention. Wiring the
 *   real accessibility label is an implementation-time task, not a design decision.)
 *
 * No other strings in this file are user-visible (the badge shows only digits).
 */
package network.bisq.mobile.presentation.design.community

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ChatIconOutlined
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ---------------------------------------------------------------------------
// Formatting helper
// ---------------------------------------------------------------------------

private const val UNREAD_BADGE_CAP = 99

/** Caps large unread counts at "99+" so the badge pill never grows unbounded. */
private fun formatUnreadBadgeCount(count: Int): String = if (count > UNREAD_BADGE_CAP) "$UNREAD_BADGE_CAP+" else count.toString()

// ---------------------------------------------------------------------------
// Component under design
// ---------------------------------------------------------------------------

/**
 * The global community entry point, rendered in TopBar's `extraActions` slot.
 *
 * @param unreadCount the GLOBAL community unread count — see "BADGE SEMANTICS" above for
 *   the exact definition (Discussions unread this milestone; + DM unread once #590
 *   ships). 0 hides the badge entirely.
 * @param showAnimation whether the badge pulses — should follow the same
 *   AnimationSettings-gated flag already used for the bottom-nav badge
 *   (see agent memory: project_animations_setting_wiring.md), not a separate toggle.
 * @param onClick navigates to `NavRoute.CommunityHub`.
 */
@Composable
internal fun CommunityTopBarIcon(
    unreadCount: Int,
    showAnimation: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.testTag("community_topbar_icon"),
    ) {
        if (unreadCount > 0) {
            BadgedBox(
                badge = {
                    // Small, near-zero offsets — see the "BUG FIX" section in this file's
                    // KDoc. AnimatedBadge's defaults are tuned for BottomNavigation's
                    // Column layout, which has more surrounding room than TopAppBar's
                    // fixed-height, clipping Surface.
                    AnimatedBadge(
                        text = formatUnreadBadgeCount(unreadCount),
                        showAnimation = showAnimation,
                        xOffset = 2.dp,
                        yOffset = (-2).dp,
                    )
                },
                modifier = Modifier.padding(top = 2.dp, end = 2.dp),
            ) {
                ChatIconOutlined(modifier = Modifier.size(BisqUIConstants.ScreenPadding2X))
            }
        } else {
            ChatIconOutlined(modifier = Modifier.size(BisqUIConstants.ScreenPadding2X))
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

/** Preview: idle state, no unread — shown in the Home tab's logo-title TopBar context. */
@ExcludeFromCoverage
@Preview(name = "Community icon — idle, Home tab context")
@Composable
private fun CommunityTopBarIcon_Idle_HomeContextPreview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "",
            isHome = true,
            showUserAvatar = true,
            extraActions = { CommunityTopBarIcon(unreadCount = 0, showAnimation = false, onClick = {}) },
        )
    }
}

/** Preview: small unread count (3), shown against a titled TopBar (Offerbook tab). */
@ExcludeFromCoverage
@Preview(name = "Community icon — 3 unread, Offerbook tab context")
@Composable
private fun CommunityTopBarIcon_SmallUnread_OfferbookContextPreview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "Offerbook",
            showBackButton = false,
            showUserAvatar = true,
            extraActions = { CommunityTopBarIcon(unreadCount = 3, showAnimation = false, onClick = {}) },
        )
    }
}

/** Preview: large unread count — verifies the "99+" cap renders without breaking the pill shape. */
@ExcludeFromCoverage
@Preview(name = "Community icon — 150 unread (caps at 99+)")
@Composable
private fun CommunityTopBarIcon_LargeUnread_CapsPreview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "My Trades",
            showUserAvatar = true,
            extraActions = { CommunityTopBarIcon(unreadCount = 150, showAnimation = false, onClick = {}) },
        )
    }
}

/** Preview: pulsing badge — new-activity animation state, gated by AnimationSettings in production. */
@ExcludeFromCoverage
@Preview(name = "Community icon — unread + pulse animation")
@Composable
private fun CommunityTopBarIcon_AnimatingPreview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "My Trades",
            showUserAvatar = true,
            extraActions = { CommunityTopBarIcon(unreadCount = 7, showAnimation = true, onClick = {}) },
        )
    }
}

/** Preview: side-by-side comparison of idle / unread / animating states for direct sign-off. */
@ExcludeFromCoverage
@Preview(name = "Community icon — state comparison")
@Composable
private fun CommunityTopBarIcon_StateComparisonPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
        ) {
            Row {
                BisqText.SmallLight("Idle:", color = BisqTheme.colors.mid_grey30)
                BisqGap.H1()
                CommunityTopBarIcon(unreadCount = 0, showAnimation = false, onClick = {})
            }
            Row {
                BisqText.SmallLight("Unread (5):", color = BisqTheme.colors.mid_grey30)
                BisqGap.H1()
                CommunityTopBarIcon(unreadCount = 5, showAnimation = false, onClick = {})
            }
            Row {
                BisqText.SmallLight("Unread + pulse:", color = BisqTheme.colors.mid_grey30)
                BisqGap.H1()
                CommunityTopBarIcon(unreadCount = 5, showAnimation = true, onClick = {})
            }
        }
    }
}

/**
 * Interactive preview: tapping the icon cycles through unread-count states
 * (0 → 3 → 150 → 0 …). Dev-only demo of the badge system; production tapping
 * navigates to NavRoute.CommunityHub instead of cycling.
 */
@ExcludeFromCoverage
@Preview(name = "Community icon — interactive (tap to cycle unread)")
@Composable
private fun CommunityTopBarIcon_InteractivePreview() {
    val cycle = listOf(0, 3, 150)
    var index by remember { mutableIntStateOf(0) }
    BisqTheme.Preview {
        TopBarContent(
            title = "Interactive demo",
            showUserAvatar = true,
            extraActions = {
                CommunityTopBarIcon(
                    unreadCount = cycle[index],
                    showAnimation = cycle[index] > 0,
                    onClick = { index = (index + 1) % cycle.size },
                )
            },
        )
    }
}

/**
 * Preview: proves the "BADGE SEMANTICS" drill-down claim — the global top-bar count is
 * exactly the SUM of the hub's per-segment counts. `CommunitySegmentedTabRow` is
 * `internal` in the SAME package (CommunityHubScreenDesign.kt), reused directly here
 * rather than duplicated. The third drill-down level (per-conversation unread) is not
 * re-rendered here to avoid duplicating that file's fixtures — see
 * `ConversationRow_WithUnreadPreview` in PrivateChatListScreenDesign.kt.
 */
@ExcludeFromCoverage
@Preview(name = "Community icon — drill-down: global badge = sum of hub segment counts")
@Composable
private fun CommunityTopBarIcon_DrillDownPreview() {
    val discussionsUnread = 3
    val messagesUnread = 2
    val globalUnread = discussionsUnread + messagesUnread
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            BisqText.SmallRegular("Level 1 — global badge (top bar, every screen):", color = BisqTheme.colors.mid_grey20)
            TopBarContent(
                title = "My Trades",
                showUserAvatar = true,
                extraActions = { CommunityTopBarIcon(unreadCount = globalUnread, showAnimation = false, onClick = {}) },
            )
            BisqGap.V1()
            BisqText.SmallRegular("Level 2 — hub segment counts (Discussions + Messages = $globalUnread):", color = BisqTheme.colors.mid_grey20)
            CommunitySegmentedTabRow(
                selected = SimulatedHubSegment.MESSAGES,
                onSelect = {},
                counts =
                    mapOf(
                        SimulatedHubSegment.DISCUSSIONS to discussionsUnread,
                        SimulatedHubSegment.MESSAGES to messagesUnread,
                        SimulatedHubSegment.CONTACTS to 0,
                    ),
            )
            BisqGap.V1()
            BisqText.SmallRegular(
                "Level 3 — per-conversation unread: see ConversationRow_WithUnreadPreview " +
                    "in PrivateChatListScreenDesign.kt.",
                color = BisqTheme.colors.mid_grey20,
            )
        }
    }
}
