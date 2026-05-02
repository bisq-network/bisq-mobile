/**
 * KeepConnectedSettingDesign.kt — Design PoC (Issue #1394)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 * Companion to the relayed-push-notifications work; the production implementation
 * goes into `SettingsScreen.kt` once approved.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * The "Keep connected in background" setting lets a user who has opted into relayed
 * (FCM/APNs) notifications additionally keep the local Android foreground service
 * running — so the WebSocket to their trusted node stays alive in background and the
 * app feels instantly-connected on reopen. Trade-off: battery cost + a persistent
 * "Bisq is running" foreground notification while the service is up.
 *
 * Three exposed states (4th combination — relayed-off + keep-connected-off — is NOT
 * exposed because it produces confusing UX "when do I even get notified?"):
 *
 *   Relayed OFF                  → FG service always runs (current default behavior).
 *                                  Keep-connected toggle is HIDDEN.
 *
 *   Relayed ON  + Keep-connected OFF (default after opt-in)
 *                                → FCM-only delivery. No FG service. No persistent
 *                                  notification. Lowest battery. WebSocket may drop
 *                                  in background; reconnect on reopen.
 *
 *   Relayed ON  + Keep-connected ON
 *                                → Power-user combo. FCM as backstop AND local FG
 *                                  service alive. WebSocket stays connected; instant
 *                                  reopen, real-time chat. Higher battery + persistent
 *                                  system notification.
 *
 * ======================================================================================
 * DESIGN DECISIONS
 * ======================================================================================
 * 1. **Hide rather than disable** when relayed is OFF.
 *    The setting is meaningless without relayed (FG service runs by default in that
 *    mode regardless), so showing a disabled control adds visual noise without any
 *    information value. Hide cleanly; appears only when its parent toggle has been
 *    turned ON. Mirrors the iOS Settings pattern of sub-options that fade in.
 *
 * 2. **Visual hierarchy via indent**, not a card or divider.
 *    A 16dp leading indent communicates "this depends on the toggle above" without
 *    introducing a heavier component. Cards would over-complicate; an indent is the
 *    minimum visual signal that does the job.
 *
 * 3. **Default = OFF after the user opts into relayed.**
 *    Users opt into relayed mode specifically to *escape* the persistent foreground
 *    notification and battery cost. Defaulting keep-connected ON would silently undo
 *    that choice. Power users who want both opt in deliberately.
 *
 * 4. **Trade-off copy: full picture in the subtitle, no "Learn more" link.**
 *    The subtitle communicates all four relevant facts:
 *      - Benefit on: trades / chats update in real-time, app opens instantly
 *        from a notification.
 *      - Cost of off: a few seconds reconnect delay when opening from a push.
 *      - Cost of on: more battery use.
 *      - Cost of on: persistent system notification while active.
 *    No mention of "WebSocket" or "foreground service" — those are implementation
 *    details. We deliberately do NOT add a "Learn more" link: there's no wiki
 *    section that goes deeper than this subtitle today, and a link to a missing
 *    target is worse than no link. If we ever grow `Push-Notifications-Android.md`
 *    to cover this in more depth, we add the link back then.
 *
 * 5. **Android Connect only.**
 *    The relayed-push toggle that gates this setting is only present on Android
 *    Connect (iOS Connect uses APNs always-on; the Node app has no relayed mode).
 *    On platforms where the parent toggle isn't shown, this setting isn't shown
 *    either — same `shouldShowPushNotificationsToggle` gating already in use.
 *
 * ======================================================================================
 * INTEGRATION NOTES (for the implementing dev)
 * ======================================================================================
 * Production wiring would touch:
 *
 *   - `SettingsUiState`: add `keepConnectedInBackground: Boolean = false`
 *   - `SettingsUiAction`: add `data class OnKeepConnectedToggle(val enabled: Boolean)`
 *   - `SettingsPresenter`:
 *       - inject something exposing this preference (likely add to
 *         `SettingsRepository.Settings` as `keepConnectedInBackground` field, default
 *         false)
 *       - `onKeepConnectedToggle` writes through; the orchestrator in
 *         `ClientApplicationLifecycleService` reads it and folds it into the
 *         existing `combine(...)` that drives `setLocalDeliverySuppressed`. New
 *         logic: `suppressed = (relayed && !keepConnected) || !osGranted`.
 *   - `SettingsScreen`: in the existing notifications section, render this
 *     composable conditionally on `uiState.pushNotificationsEnabled`.
 *
 * i18n keys to add (sentence case, matching existing convention):
 *
 *   - `mobile.pushNotifications.settings.keepConnected.toggleLabel`
 *       = "Keep connected in background"
 *   - `mobile.pushNotifications.settings.keepConnected.subtitle`
 *       = "Trades and chats update in real-time, and the app opens instantly
 *          from notifications. Without this, expect a few seconds to reconnect
 *          when you open the app from a push. Costs more battery and shows
 *          a persistent system notification while active."
 *
 * Future enhancement: if we grow `Push-Notifications-Android.md` to cover this
 * setting in more depth (battery measurements, network behaviour, etc.), add a
 * "How background connection works \u2192" link beneath the subtitle and a
 * matching `keepConnected.learnMoreLink` i18n key. For now, the subtitle alone
 * carries the full trade-off and there's nothing the link would point to.
 */

@file:Suppress("MagicNumber")

package network.bisq.mobile.presentation.design.push_notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSwitch
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// =====================================================================================
// CORE COMPONENT — what would be extracted into production
// =====================================================================================

/**
 * The "Keep connected in background" sub-setting. Renders as an indented switch with a
 * trade-off subtitle that covers all four relevant facts (the two benefits of
 * enabling, the cost of leaving it off, the cost of having it on).
 *
 * Should only be rendered when the user has already opted into relayed push
 * notifications — see [NotificationsSectionWithKeepConnected] for the parent-context
 * pattern.
 */
@Composable
fun KeepConnectedSetting(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                // Indent communicates "depends on the toggle above" without adding a
                // heavier visual element (card / divider). Matches the iOS Settings
                // sub-option pattern.
                .padding(start = BisqUIConstants.ScreenPadding)
                .fillMaxWidth(),
    ) {
        BisqSwitch(
            label = "Keep connected in background",
            checked = enabled,
            onSwitch = onToggle,
        )

        BisqGap.VQuarter()

        BisqText.SmallLight(
            text =
                "Trades and chats update in real-time, and the app opens instantly " +
                    "from notifications. Without this, expect a few seconds to reconnect " +
                    "when you open the app from a push. Costs more battery and shows " +
                    "a persistent system notification while active.",
            color = BisqTheme.colors.mid_grey20,
        )
    }
}

// =====================================================================================
// PARENT CONTEXT — shows how this lives inside the existing notifications section
// =====================================================================================

/**
 * Stand-in for the production notifications section in `SettingsScreen`. Mirrors the
 * existing relayed-push toggle + subtitle + learn-more pattern, then conditionally
 * inserts [KeepConnectedSetting] when relayed is on.
 *
 * This is what the design POC is asking the team to evaluate — the integrated look,
 * not just the new switch in isolation.
 */
@Composable
fun NotificationsSectionWithKeepConnected(
    relayedEnabled: Boolean,
    onRelayedToggle: (Boolean) -> Unit,
    keepConnectedEnabled: Boolean,
    onKeepConnectedToggle: (Boolean) -> Unit,
    onRelayedLearnMoreClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BisqHDivider()

        BisqText.H4Light("Notifications")

        BisqGap.V1()

        // Existing relayed-push toggle — unchanged. Reproduced here for layout.
        BisqSwitch(
            label = "Relayed push notifications",
            checked = relayedEnabled,
            onSwitch = onRelayedToggle,
        )

        BisqGap.VQuarter()

        BisqText.SmallLight(
            text = "Receive trade alerts when the app is closed",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.VHalf()

        BisqText.SmallLight(
            text = "How this works and what is shared \u2192",
            color = BisqTheme.colors.primary,
            modifier = Modifier.clickable { onRelayedLearnMoreClick() },
        )

        // The new sub-setting. Only visible when relayed is on. We use a slight gap
        // above to give the dependency relationship room to breathe.
        if (relayedEnabled) {
            BisqGap.V1()
            KeepConnectedSetting(
                enabled = keepConnectedEnabled,
                onToggle = onKeepConnectedToggle,
            )
        } else {
            // When relayed is off, render the existing "you may miss messages" warning
            // exactly as today — keep-connected has no effect in this mode (the FG
            // service runs by default), so we don't surface it.
            BisqGap.VHalf()
            BisqText.SmallLight(
                text = "You may miss trade messages while the app is closed.",
                color = BisqTheme.colors.warning,
            )
        }
    }
}

// =====================================================================================
// PREVIEWS — drive design review with these
// =====================================================================================

@ExcludeFromCoverage
@Preview
@Composable
private fun NotificationsSection_RelayedOff_Preview() {
    // Relayed OFF: the keep-connected toggle is hidden entirely. The user sees the
    // existing "you may miss messages" warning. This is what most users on Android
    // Connect see by default, and the only state Node-app + iOS users can ever see.
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            NotificationsSectionWithKeepConnected(
                relayedEnabled = false,
                onRelayedToggle = {},
                keepConnectedEnabled = false,
                onKeepConnectedToggle = {},
                onRelayedLearnMoreClick = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NotificationsSection_RelayedOn_KeepConnectedOff_Preview() {
    // Relayed ON, keep-connected OFF — the default after opting in. This is the
    // pure-relayed experience: no FG service, no persistent notification, FCM-only
    // delivery. Lowest battery cost. Most users who opt in stay here.
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            NotificationsSectionWithKeepConnected(
                relayedEnabled = true,
                onRelayedToggle = {},
                keepConnectedEnabled = false,
                onKeepConnectedToggle = {},
                onRelayedLearnMoreClick = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NotificationsSection_RelayedOn_KeepConnectedOn_Preview() {
    // Relayed ON + keep-connected ON — the power-user combo. FG service runs (so the
    // persistent system notification is visible) AND FCM is the backstop. Best UX
    // continuity: instant reopen, real-time chat. Highest battery cost.
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            NotificationsSectionWithKeepConnected(
                relayedEnabled = true,
                onRelayedToggle = {},
                keepConnectedEnabled = true,
                onKeepConnectedToggle = {},
                onRelayedLearnMoreClick = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun KeepConnectedSetting_InIsolation_Off_Preview() {
    // Just the new component in isolation, off state — for component-review without
    // surrounding context.
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            KeepConnectedSetting(
                enabled = false,
                onToggle = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun KeepConnectedSetting_InIsolation_On_Preview() {
    // Same, on state.
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            KeepConnectedSetting(
                enabled = true,
                onToggle = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NotificationsSection_Interactive_Preview() {
    // Interactive preview — toggle both switches in the IDE preview to verify the
    // visibility transition and the visual rhythm of the section.
    BisqTheme.Preview {
        var relayed by remember { mutableStateOf(false) }
        var keepConnected by remember { mutableStateOf(false) }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            NotificationsSectionWithKeepConnected(
                relayedEnabled = relayed,
                onRelayedToggle = {
                    relayed = it
                    if (!it) keepConnected = false // hide-implies-reset semantics for the preview
                },
                keepConnectedEnabled = keepConnected,
                onKeepConnectedToggle = { keepConnected = it },
                onRelayedLearnMoreClick = {},
            )
        }
    }
}
