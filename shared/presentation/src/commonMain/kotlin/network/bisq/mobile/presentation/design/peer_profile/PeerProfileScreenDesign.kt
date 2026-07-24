/**
 * PeerProfileScreenDesign.kt — Design PoC (Milestone 11 "Bisq community", issue #545)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * The net-new "peer" primitive from the milestone-11 IA recommendation (see agent
 * memory: project_milestone11_community_ia.md). This screen is never itself a nav
 * destination with its own home — it is always entered "about someone", from any place
 * a peer's identity appears. See the ENTRY POINTS section below for the full, decided
 * list and example previews.
 *
 * ======================================================================================
 * DESKTOP REFERENCE
 * ======================================================================================
 * bisq2/apps/desktop/desktop/src/main/java/bisq/desktop/main/content/user/profile_card/
 * — ProfileCardView/Controller/Model plus sub-packages `overview`, `details`,
 * `reputation`, `offers`, `messages`, `my_notes`. Mobile scope for this milestone covers
 * the `overview` + `reputation` equivalent, plus the ignore/report actions that desktop
 * also hosts here. `offers` / `messages` / `my_notes` equivalents are out of scope for
 * this pass (offers → "Trade again" is a single visual hook, not the full offers list —
 * see TRADE AGAIN section; messages/notes are meaningfully blocked on #590/#1238).
 *
 * ======================================================================================
 * IGNORE / UNIGNORE — VISIBLE ON THE SCREEN (review pass, rodvar's #4)
 * ======================================================================================
 * Ignore/undo-ignore is now a VISIBLE, always-reachable button in the screen body
 * (`PeerProfileActionButtons`, third action below "Trade again"), not only tucked in the
 * overflow menu. Rationale: blocking/unblocking a peer is a trust-and-safety decision a
 * user should be able to find without discovering a hidden "⋮" affordance first — this
 * mirrors how WhatsApp/Telegram surface Block/Unblock as a plain, labelled row on the
 * contact screen rather than only inside an overflow menu. The overflow menu keeps
 * Report ONLY (see REPORT section below) — reporting is comparatively rare and
 * consequential enough that a secondary, less-prominent placement is appropriate,
 * whereas ignore/unignore is common enough (and reversible enough) to deserve a
 * first-class, always-visible control.
 *
 * `ChatMessageContextMenu` (molecules/chat/ChatMessageContextMenu.kt) remains a
 * convenience shortcut for ignore/undo-ignore/report from within a message's long-press
 * menu — this screen is simply the canonical, always-reachable home, per the original
 * IA decision. It already references the i18n key `user.profileCard.userActions.
 * undoIgnore` — someone anticipated a profile card before one existed; this is that card.
 *
 * ======================================================================================
 * REPORT — RESOLVED, WIREABLE (was an open question; investigation closed it)
 * ======================================================================================
 * Previously flagged here as an open engineering question because
 * `ReportUserPresenter.initialize(chatMessage: BisqEasyOpenTradeMessageModel, ...)`
 * requires a specific chat message. Investigation (rodvar) found this is NOT a backend
 * blocker:
 *   - Backend: `POST /user-profiles/report/{profileId}` + core
 *     `ModerationRequestService.reportUserProfile(UserProfile, message)` — both take
 *     only the target profile's id and an optional message, no chat-message reference.
 *   - Desktop precedent: the profile card ALREADY reports with no message —
 *     `ProfileCardController.onReportUser` → `ReportToModeratorWindow(userProfile)`.
 *
 * So reporting a peer independent of any specific chat message is a real, existing
 * capability — the gap is only that mobile's `ReportUserPresenter.initialize()` was
 * written assuming a trade-chat message is always available (because until this screen
 * existed, it always was). Implementing `OnReportClick` here needs a thin presenter
 * refactor — broaden `initialize()` to accept a `UserProfileVO` / profile id directly
 * (with the existing chat-message path becoming one way of obtaining that id, not the
 * only way) — not a new backend endpoint or a bisq2-side decision.
 *
 * ======================================================================================
 * "SEND PRIVATE MESSAGE" — VISUAL-ONLY THIS MILESTONE, TARGETS THE DM SCREEN
 * ======================================================================================
 * Private DMs (#590) are fast-follow. This screen already imports and reuses the REAL
 * `OpenPrivateChatTextButton` component directly (design/community/private_chat/
 * OpenPrivateChatButtonDesign.kt) — not a redrawn stand-in — passed `disabled = true`
 * with a "coming soon" label via its `disabledLabel` param. When #590 ships, this
 * becomes `OpenPrivateChatTextButton(isLoading = ..., onClick = ...)` with no visual
 * change and no redesign. The screen it navigates to is
 * `design/community/private_chat/PrivateChatScreenDesign.kt`'s `PrivateChatScreenContent`
 * — see that file's KDoc, which documents this screen as its entry point.
 *
 * ======================================================================================
 * "TRADE AGAIN" — VISUAL-ONLY STUB THIS MILESTONE, TRACKED SEPARATELY
 * ======================================================================================
 * Renders as a disabled "Trade again (soon)" stub this milestone, parallel to the
 * messaging action. The REAL feature — viewing/browsing the specific peer's active
 * offers from their profile — is tracked as its own follow-up, not bundled into #545:
 * see `docs/design/milestone11/issue-trade-again-peer-offers.md` (GitHub issue TBD,
 * rodvar to file). Desktop precedent for the real feature: the profile card's `offers`
 * sub-package (bisq2/.../user/profile_card/offers/). Do not scope-creep this screen to
 * build that list now — the stub button is the intentional milestone-11 boundary.
 *
 * ======================================================================================
 * ENTRY POINTS (rodvar's #6 — decided list + example previews)
 * ======================================================================================
 * Every peer-identity render in the app should be tappable → this screen. Decided list:
 *   - Offerbook offer row (avatar + author name) — see
 *     `PeerProfileEntryPoint_OfferbookRowPreview` below for a worked example.
 *   - Chat message avatar/name, public channel or DM — see
 *     `PeerProfileEntryPoint_ChatAvatarPreview` below. This is the SAME requirement
 *     generalized from DiscussionsChannelScreenDesign.kt's `OnAvatarClick` gap (that
 *     file documents the concrete `ChatTextMessageBox`/`ChatMessageList` signature
 *     change needed) — the two previews here exist to make the pattern legible outside
 *     the chat-specific file, not to re-derive it.
 *   - Trade peer header (OpenTradeScreen's UserProfileRow for the counterparty).
 *   - Ignored Users list rows (settings/ignored_users/IgnoredUsersScreen.kt) — today a
 *     dead-end static list with no tap-through; each row should deep-link here.
 *   - Contacts directory rows (#1238, fast-follow, not designed yet).
 *
 * This is a UI-wide convention, not a per-screen decision — any new surface that renders
 * a peer's avatar/name should default to wiring this tap-through unless there's a
 * specific reason not to (e.g. the local user's own profile — see OWN-PROFILE GUARD).
 *
 * ======================================================================================
 * OWN-PROFILE GUARD
 * ======================================================================================
 * This screen should never be reachable for the local user's own profile (that's the
 * existing "User Profile" entry in More). `isOwnProfile = true` renders a guard message
 * instead of the profile body — mirrors the defensive pattern already established by
 * `OpenPrivateChatButton_OwnProfile_RenderNothingPreview` in design/community/
 * private_chat/OpenPrivateChatButtonDesign.kt. In production this should be an
 * assertion / earlier navigation guard, not a real reachable UI state — the preview
 * exists to document the expectation for implementers.
 *
 * ======================================================================================
 * LAYOUT
 * ======================================================================================
 * ┌─────────────────────────────────────────┐
 * │ TopBar: "← SatoshiFan#1234"          ⋮  │  ⋮ = overflow: Report ONLY
 * ├─────────────────────────────────────────┤
 * │ [You have ignored this user.  Undo]      │  only if isIgnored
 * ├─────────────────────────────────────────┤
 * │              (avatar, 72dp)              │
 * │            SatoshiFan#1234               │
 * │              ★★★★☆                       │
 * │         Reputation: 12,400 pts           │
 * │         Member for 8 months              │
 * │         Traded with you 3 times          │
 * ├─────────────────────────────────────────┤
 * │ [ Send private message (soon) ]          │
 * │ [ Trade again (soon) ]                   │
 * │ [ 👁 Ignore user ]  ← visible, not menu   │
 * └─────────────────────────────────────────┘
 *
 * ======================================================================================
 * i18n KEYS NEEDED
 * ======================================================================================
 * mobile.peerProfile.reputation             → "Reputation: {0} pts"
 * mobile.peerProfile.memberFor               → "Member for {0}"
 * mobile.peerProfile.tradedWithYou            → "Traded with you {0} times"
 * mobile.peerProfile.tradedWithYou.zero       → "No trades with you yet"
 * mobile.peerProfile.action.tradeAgain         → "Trade again"
 * mobile.peerProfile.action.tradeAgain.soon    → "Trade again (soon)"
 * mobile.peerProfile.ignoredBanner             → "You have ignored this user."
 * mobile.peerProfile.ignoredBanner.undo        → "Undo"
 * mobile.peerProfile.action.ignore             → existing key:
 *   chat.message.contextMenu.ignoreUser — REUSE, do not duplicate
 * mobile.peerProfile.action.undoIgnore         → existing key:
 *   user.profileCard.userActions.undoIgnore — REUSE, do not duplicate
 * mobile.peerProfile.menu.report               → existing key:
 *   chat.message.contextMenu.reportUser — REUSE, do not duplicate
 * mobile.peerProfile.ownProfileGuard           → "This is your own profile."
 * (Send-private-message strings live in OpenPrivateChatButtonDesign.kt — reused, not
 * duplicated here.)
 *
 * ======================================================================================
 * TEXT EXPANSION
 * ======================================================================================
 * "Reputation: 12,400 pts" in German → "Reputation: 12.400 Pkt." (similar length, note
 * the locale-specific thousands separator — number formatting must be locale-aware, not
 * hardcoded comma grouping). "Member for 8 months" → "Mitglied seit 8 Monaten" (~20%
 * longer) — kept on its own line with no fixed-width container so it wraps safely.
 */
package network.bisq.mobile.presentation.design.peer_profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ClosedEyeIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.EyeIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.FlagIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.design.community.private_chat.OpenPrivateChatTextButton

// ============================================================================================
// Simulated data — no domain type dependencies
// ============================================================================================

internal data class SimulatedPeerProfile(
    val displayName: String,
    val starRating: Double,
    val reputationPoints: Int,
    val memberForLabel: String,
    val tradeCountWithPeer: Int,
    val isIgnored: Boolean,
)

// ============================================================================================
// UiState / UiAction
// ============================================================================================

internal data class PeerProfileUiState(
    val profile: SimulatedPeerProfile,
    val isOwnProfile: Boolean = false,
    val isMessagingAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val showOverflowMenu: Boolean = false,
    val showIgnoreConfirmDialog: Boolean = false,
)

internal sealed interface PeerProfileUiAction {
    data object OnToggleOverflowMenu : PeerProfileUiAction

    data object OnMessageClick : PeerProfileUiAction

    data object OnTradeAgainClick : PeerProfileUiAction

    data object OnIgnoreClick : PeerProfileUiAction

    data object OnConfirmIgnore : PeerProfileUiAction

    data object OnDismissIgnoreDialog : PeerProfileUiAction

    data object OnUndoIgnoreClick : PeerProfileUiAction

    data object OnReportClick : PeerProfileUiAction
}

// ============================================================================================
// Content
// ============================================================================================

@Composable
internal fun PeerProfileScreenContent(
    uiState: PeerProfileUiState,
    onAction: (PeerProfileUiAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(BisqTheme.colors.backgroundColor)) {
        TopBarContent(
            title = uiState.profile.displayName,
            showBackButton = true,
            showUserAvatar = false,
            extraActions = {
                // Overflow keeps Report ONLY — ignore/unignore moved to a visible body
                // button, see file KDoc "IGNORE / UNIGNORE — VISIBLE ON THE SCREEN".
                if (!uiState.isOwnProfile) {
                    Box {
                        IconButton(onClick = { onAction(PeerProfileUiAction.OnToggleOverflowMenu) }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = BisqTheme.colors.mid_grey30)
                        }
                        DropdownMenu(
                            expanded = uiState.showOverflowMenu,
                            onDismissRequest = { onAction(PeerProfileUiAction.OnToggleOverflowMenu) },
                            containerColor = BisqTheme.colors.dark_grey40,
                        ) {
                            DropdownMenuItem(
                                text = { BisqText.SmallRegular("Report user") },
                                leadingIcon = { FlagIcon() },
                                onClick = { onAction(PeerProfileUiAction.OnReportClick) },
                            )
                        }
                    }
                }
            },
        )

        when {
            uiState.isOwnProfile -> PeerProfileOwnProfileGuard()
            uiState.isLoading -> PeerProfileLoadingState()
            else -> {
                if (uiState.profile.isIgnored) {
                    PeerProfileIgnoredBanner(onUndo = { onAction(PeerProfileUiAction.OnUndoIgnoreClick) })
                }
                PeerProfileBody(
                    profile = uiState.profile,
                    isMessagingAvailable = uiState.isMessagingAvailable,
                    onMessageClick = { onAction(PeerProfileUiAction.OnMessageClick) },
                    onTradeAgainClick = { onAction(PeerProfileUiAction.OnTradeAgainClick) },
                    onIgnoreClick = { onAction(PeerProfileUiAction.OnIgnoreClick) },
                    onUndoIgnoreClick = { onAction(PeerProfileUiAction.OnUndoIgnoreClick) },
                )
            }
        }

        if (uiState.showIgnoreConfirmDialog) {
            ConfirmationDialog(
                headline = "Ignore this user?",
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = "You will no longer see messages or offers from this user. You can undo this at any time.",
                confirmButtonText = "Ignore user",
                dismissButtonText = "Cancel",
                verticalButtonPlacement = true,
                onConfirm = { onAction(PeerProfileUiAction.OnConfirmIgnore) },
                onDismiss = { _ -> onAction(PeerProfileUiAction.OnDismissIgnoreDialog) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Body sections
// ---------------------------------------------------------------------------

@Composable
private fun PeerProfileBody(
    profile: SimulatedPeerProfile,
    isMessagingAvailable: Boolean,
    onMessageClick: () -> Unit,
    onTradeAgainClick: () -> Unit,
    onIgnoreClick: () -> Unit,
    onUndoIgnoreClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding2X),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(BisqTheme.colors.dark_grey50, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BisqText.H4Light(text = profile.displayName.first().toString(), color = BisqTheme.colors.mid_grey30)
        }
        BisqGap.V1()
        BisqText.H5Regular(text = profile.displayName, color = BisqTheme.colors.white, textAlign = TextAlign.Center)
        BisqGap.VHalf()
        StarRating(rating = profile.starRating)
        BisqGap.VHalf()
        BisqText.BaseLightGrey(text = "Reputation: ${profile.reputationPoints} pts", textAlign = TextAlign.Center)
        BisqGap.VQuarter()
        BisqText.SmallRegularGrey(text = "Member for ${profile.memberForLabel}", textAlign = TextAlign.Center)
        BisqGap.VQuarter()
        BisqText.SmallRegularGrey(
            text =
                if (profile.tradeCountWithPeer > 0) {
                    "Traded with you ${profile.tradeCountWithPeer} times"
                } else {
                    "No trades with you yet"
                },
            textAlign = TextAlign.Center,
        )

        BisqGap.V2()

        // Reuses the REAL OpenPrivateChatTextButton (design/community/private_chat/
        // OpenPrivateChatButtonDesign.kt), not a redrawn stand-in — see file KDoc.
        OpenPrivateChatTextButton(
            isLoading = false,
            disabled = !isMessagingAvailable,
            disabledLabel = "Send private message (soon)",
            onClick = onMessageClick,
        )

        BisqGap.VHalf()
        BisqButton(
            text = "Trade again (soon)",
            onClick = onTradeAgainClick,
            type = BisqButtonType.Outline,
            disabled = true,
            fullWidth = true,
        )

        BisqGap.VHalf()
        PeerProfileIgnoreButton(isIgnored = profile.isIgnored, onIgnoreClick = onIgnoreClick, onUndoIgnoreClick = onUndoIgnoreClick)
    }
}

/**
 * The visible, always-reachable ignore/unignore control (rodvar's #4). Uses a neutral
 * `GreyOutline` treatment rather than a `Danger`/`WarningOutline` one — ignoring is fully
 * reversible and common enough that it shouldn't visually read as scary or irreversible;
 * the actual warning colour is reserved for the confirmation dialog, matching how
 * `ConfirmationDialog` already treats this action elsewhere in the app.
 */
@Composable
private fun PeerProfileIgnoreButton(
    isIgnored: Boolean,
    onIgnoreClick: () -> Unit,
    onUndoIgnoreClick: () -> Unit,
) {
    BisqButton(
        text = if (isIgnored) "Undo ignore" else "Ignore user",
        onClick = if (isIgnored) onUndoIgnoreClick else onIgnoreClick,
        type = BisqButtonType.GreyOutline,
        leftIcon = { if (isIgnored) EyeIcon(modifier = Modifier.size(18.dp)) else ClosedEyeIcon(modifier = Modifier.size(18.dp)) },
        fullWidth = true,
    )
}

@Composable
private fun PeerProfileIgnoredBanner(onUndo: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.warning.copy(alpha = 0.12f))
                .padding(BisqUIConstants.ScreenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
            WarningIcon(modifier = Modifier.size(18.dp))
            BisqText.SmallRegular(text = "You have ignored this user.", color = BisqTheme.colors.warning)
        }
        BisqButton(
            text = "Undo",
            onClick = onUndo,
            type = BisqButtonType.Clear,
            color = BisqTheme.colors.warning,
        )
    }
}

@Composable
private fun PeerProfileLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BisqTheme.colors.primary, strokeWidth = 2.dp)
    }
}

@Composable
private fun PeerProfileOwnProfileGuard() {
    Box(
        modifier = Modifier.fillMaxSize().padding(BisqUIConstants.ScreenPadding2X),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.BaseLight(
            text = "This is your own profile.",
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

// ============================================================================================
// Preview fixtures
// ============================================================================================

private fun simulatedTrustedPeer() =
    SimulatedPeerProfile(
        displayName = "SatoshiFan#1234",
        starRating = 4.5,
        reputationPoints = 12400,
        memberForLabel = "8 months",
        tradeCountWithPeer = 3,
        isIgnored = false,
    )

private fun simulatedNewPeer() =
    SimulatedPeerProfile(
        displayName = "NewTrader#0007",
        starRating = 0.5,
        reputationPoints = 120,
        memberForLabel = "2 days",
        tradeCountWithPeer = 0,
        isIgnored = false,
    )

private fun simulatedIgnoredPeer() =
    SimulatedPeerProfile(
        displayName = "SuspiciousUser#9999",
        starRating = 1.0,
        reputationPoints = 340,
        memberForLabel = "3 weeks",
        tradeCountWithPeer = 1,
        isIgnored = true,
    )

// ============================================================================================
// Previews
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "Peer Profile — Trusted, high-reputation peer")
@Composable
private fun PeerProfileScreen_TrustedPeerPreview() {
    BisqTheme.Preview {
        PeerProfileScreenContent(uiState = PeerProfileUiState(profile = simulatedTrustedPeer()), onAction = {})
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer Profile — New trader, low reputation")
@Composable
private fun PeerProfileScreen_NewPeerPreview() {
    BisqTheme.Preview {
        PeerProfileScreenContent(uiState = PeerProfileUiState(profile = simulatedNewPeer()), onAction = {})
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer Profile — Ignored peer (banner + visible Undo button + inline banner Undo)")
@Composable
private fun PeerProfileScreen_IgnoredPeerPreview() {
    BisqTheme.Preview {
        PeerProfileScreenContent(uiState = PeerProfileUiState(profile = simulatedIgnoredPeer()), onAction = {})
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer Profile — Loading")
@Composable
private fun PeerProfileScreen_LoadingPreview() {
    BisqTheme.Preview {
        PeerProfileScreenContent(uiState = PeerProfileUiState(profile = simulatedTrustedPeer(), isLoading = true), onAction = {})
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer Profile — Overflow menu open (Report only), not ignored")
@Composable
private fun PeerProfileScreen_OverflowMenu_NotIgnoredPreview() {
    BisqTheme.Preview {
        PeerProfileScreenContent(
            uiState = PeerProfileUiState(profile = simulatedTrustedPeer(), showOverflowMenu = true),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer Profile — Overflow menu open (Report only), already ignored")
@Composable
private fun PeerProfileScreen_OverflowMenu_IgnoredPreview() {
    BisqTheme.Preview {
        PeerProfileScreenContent(
            uiState = PeerProfileUiState(profile = simulatedIgnoredPeer(), showOverflowMenu = true),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer Profile — Ignore confirmation dialog (triggered from the visible body button)")
@Composable
private fun PeerProfileScreen_IgnoreConfirmDialogPreview() {
    BisqTheme.Preview {
        PeerProfileScreenContent(
            uiState = PeerProfileUiState(profile = simulatedTrustedPeer(), showIgnoreConfirmDialog = true),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer Profile — Own-profile guard (should never be reachable in production)")
@Composable
private fun PeerProfileScreen_OwnProfileGuardPreview() {
    BisqTheme.Preview {
        PeerProfileScreenContent(
            uiState = PeerProfileUiState(profile = simulatedTrustedPeer(), isOwnProfile = true),
            onAction = {},
        )
    }
}

/**
 * Preview: what this screen looks like once #590 (private DMs) ships and
 * `isMessagingAvailable` flips to true — the "Send private message" button becomes
 * enabled with no other layout change, confirming the visual-consistency claim above.
 */
@ExcludeFromCoverage
@Preview(name = "Peer Profile — Messaging available (post-#590 fast-follow state)")
@Composable
private fun PeerProfileScreen_MessagingAvailablePreview() {
    BisqTheme.Preview {
        PeerProfileScreenContent(
            uiState = PeerProfileUiState(profile = simulatedTrustedPeer(), isMessagingAvailable = true),
            onAction = {},
        )
    }
}

/**
 * Preview: the visible ignore/unignore button in isolation, both states side by side,
 * for direct sign-off on the styling decision described in the file KDoc.
 */
@ExcludeFromCoverage
@Preview(name = "Peer Profile — Ignore button, both states isolated")
@Composable
private fun PeerProfileIgnoreButton_BothStatesPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.background(BisqTheme.colors.backgroundColor).padding(BisqUIConstants.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BisqText.SmallRegular("Not ignored:", color = BisqTheme.colors.mid_grey20)
            PeerProfileIgnoreButton(isIgnored = false, onIgnoreClick = {}, onUndoIgnoreClick = {})
            BisqGap.V1()
            BisqText.SmallRegular("Already ignored:", color = BisqTheme.colors.mid_grey20)
            PeerProfileIgnoreButton(isIgnored = true, onIgnoreClick = {}, onUndoIgnoreClick = {})
        }
    }
}

// ============================================================================================
// ENTRY POINT examples — see file KDoc "ENTRY POINTS"
// ============================================================================================

/**
 * Preview: an offerbook offer row where the peer's avatar and name are the tap target
 * into this screen. Illustrative only — the real offer-row composable lives elsewhere;
 * this demonstrates the generalized peer-identity tap-through convention decided in the
 * file KDoc, not a redesign of the offer row itself.
 */
@ExcludeFromCoverage
@Preview(name = "Entry point — Offerbook offer row → Peer Profile")
@Composable
private fun PeerProfileEntryPoint_OfferbookRowPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor).padding(BisqUIConstants.ScreenPadding)) {
            BisqText.SmallRegular("Offerbook offer row (avatar + name tappable):", color = BisqTheme.colors.mid_grey20)
            BisqGap.VHalf()
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
                    // This whole Row (avatar + name) is the tap target → Peer Profile.
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                BisqText.BaseRegular("0.015 BTC", color = BisqTheme.colors.primary)
            }
        }
    }
}

/**
 * Preview: a chat message row where the sender's avatar/name is the tap target into this
 * screen — the generalized version of DiscussionsChannelScreenDesign.kt's
 * `OnAvatarClick`. Kept local/simplified here (not importing that file's private
 * `SimulatedChannelMessageBubble`) since this preview's only job is to make the
 * cross-cutting convention legible, not to re-render that file's exact bubble.
 */
@ExcludeFromCoverage
@Preview(name = "Entry point — Chat message avatar → Peer Profile")
@Composable
private fun PeerProfileEntryPoint_ChatAvatarPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor).padding(BisqUIConstants.ScreenPadding)) {
            BisqText.SmallRegular("Chat message row (avatar + sender name tappable):", color = BisqTheme.colors.mid_grey20)
            BisqGap.VHalf()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
                Box(
                    Modifier.size(24.dp).background(BisqTheme.colors.dark_grey50, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    BisqText.XSmallMedium(text = "S", color = BisqTheme.colors.mid_grey30)
                }
                BisqText.SmallRegular("SatoshiFan#1234", color = BisqTheme.colors.light_grey10)
                BisqText.SmallRegular("10:02", color = BisqTheme.colors.mid_grey20)
            }
            BisqGap.VQuarter()
            Box(
                modifier =
                    Modifier
                        .background(BisqTheme.colors.dark_grey40, shape = RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .padding(BisqUIConstants.ScreenPadding),
            ) {
                BisqText.BaseLight(text = "Anyone had luck with SEPA transfers over 500 EUR?", color = BisqTheme.colors.white)
            }
        }
    }
}
