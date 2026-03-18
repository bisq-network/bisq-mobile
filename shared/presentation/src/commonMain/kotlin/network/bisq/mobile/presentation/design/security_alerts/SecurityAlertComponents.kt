/**
 * # SecurityAlertComponents.kt — Design PoC (Issue #1128)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 * Copy into a production package during implementation and wire to an AlertsPresenter.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Reusable alert UI atoms and molecules for the Security Manager alert system.
 * The security manager (an authorized bonded role on the Bisq P2P network) can broadcast
 * signed [AuthorizedAlertData] messages scoped to a specific [AppType]. Three alert
 * types require mobile UI: INFO, WARN, and EMERGENCY.
 *
 * ======================================================================================
 * DOMAIN MODEL SUMMARY (bisq2/bonded-roles/…/security_manager/alert/)
 * ======================================================================================
 * AuthorizedAlertData fields relevant to mobile UI:
 *   - alertType: AlertType (INFO | WARN | EMERGENCY | BAN | BANNED_ACCOUNT_DATA)
 *   - headline: Optional<String> — defaults to i18n key per type if absent
 *   - message: Optional<String> — body text, max 1000 chars
 *   - haltTrading: Boolean — if true, all new trades must be blocked (EMERGENCY only)
 *   - requireVersionForTrading: Boolean — if true, the minVersion gate applies
 *   - minVersion: Optional<String> — minimum app version required to trade
 *   - appType: AppType — DESKTOP | MOBILE | ALL (only show alerts scoped to mobile or all)
 *
 * AlertType.BAN and AlertType.BANNED_ACCOUNT_DATA are handled in Bisq core; no mobile UI.
 * AlertType.isMessageAlert() is true for INFO, WARN, and EMERGENCY.
 *
 * ======================================================================================
 * DESKTOP REFERENCE
 * ======================================================================================
 * Desktop (AlertBannerView.java):
 *   - A dismissible banner that slides in from the right, positioned above the main content.
 *   - One banner shown at a time: the most severe/most-recent alert is displayed first.
 *   - The banner uses CSS classes: "info-banner" | "warn-banner" | "emergency-banner".
 *   - Layout: [broadcast icon] [headline] [message] [close button].
 *   - Close calls AlertBannerController.onClose() which dismisses to the next queued alert.
 *
 * Mobile adaptation differences:
 *   - Persistent banner pinned below the TopBar (not overlaid on content) — this is safer
 *     on smaller screens where overlay banners obscure actionable content.
 *   - EMERGENCY alerts additionally show a modal dialog when trading is halted or a version
 *     gate blocks the user. The banner alone is insufficient for a blocking condition.
 *   - Multiple pending alerts: if >1 alert is queued, a small badge counter is shown in the
 *     banner. Dismissing reveals the next alert (same priority ordering as desktop).
 *
 * ======================================================================================
 * ALERT SEVERITY COLOR MAPPING
 * ======================================================================================
 * INFO      → BisqTheme.colors.primary (green)      — informational, low urgency
 * WARN      → BisqTheme.colors.warning (amber)       — elevated attention needed
 * EMERGENCY → BisqTheme.colors.danger  (red)         — critical, may block trading
 *
 * Rationale: Bisq's color system already has semantic green/amber/red. Reusing them
 * ensures the alert banner feels native to the design language and avoids introducing
 * new brand colors for a rare but important surface.
 *
 * ======================================================================================
 * I18N KEYS NEEDED (new, not yet in property files)
 * ======================================================================================
 * mobile.alerts.info.defaultHeadline     = "Network Information"
 * mobile.alerts.warn.defaultHeadline     = "Network Warning"
 * mobile.alerts.emergency.defaultHeadline = "Emergency Alert"
 * mobile.alerts.dismiss                  = "Dismiss"
 * mobile.alerts.updateRequired           = "Update Required"
 * mobile.alerts.updateRequired.message   = "Trading requires app version {0} or newer. Please update to continue trading."
 * mobile.alerts.updateNow                = "Update Now"
 * mobile.alerts.haltTrading.notice       = "New trades are suspended until this alert is lifted."
 * mobile.alerts.moreAlerts              = "+{0} more"
 *
 * ======================================================================================
 * TEXT EXPANSION NOTES
 * ======================================================================================
 * "Network Warning" (14 chars EN) → "Netzwerkwarnung" (15 chars DE) — fine.
 * "Emergency Alert" (15 chars EN) → "Notfallwarnung" (14 chars DE) — fine.
 * Banner headline row is single-line with ellipsis overflow; body wraps freely.
 * The "Update Now" button uses short text intentionally to leave space for expansion.
 */
package network.bisq.mobile.presentation.design.security_alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

// ---------------------------------------------------------------------------
// UiState / UiAction — production-ready patterns for implementors
// ---------------------------------------------------------------------------

/**
 * Represents a single displayable alert from the security manager.
 *
 * All fields use primitives to allow direct use in previews and easy
 * mapping from the domain VO without domain type leakage into the UI.
 *
 * @param id Unique alert ID from AuthorizedAlertData — used for dismissal deduplication.
 * @param alertLevel One of [AlertLevel] — drives color, icon, and dismissibility.
 * @param headline Short headline text. Falls back to a per-type default on the presenter side.
 * @param message Optional longer body text. May be blank.
 * @param haltTrading True only for EMERGENCY alerts when the security manager has suspended trading.
 * @param requiresUpdate True when the EMERGENCY alert enforces a minimum app version.
 * @param minVersion The minimum required version string, e.g. "2.1.8". Empty if not applicable.
 */
internal data class SecurityAlertUiState(
    val id: String,
    val alertLevel: AlertLevel,
    val headline: String,
    val message: String,
    val haltTrading: Boolean = false,
    val requiresUpdate: Boolean = false,
    val minVersion: String = "",
)

/**
 * Alert severity level as understood by the mobile UI layer.
 *
 * Mapped from [bisq.bonded_roles.security_manager.alert.AlertType]:
 *   - AlertType.INFO      → [AlertLevel.INFO]
 *   - AlertType.WARN      → [AlertLevel.WARN]
 *   - AlertType.EMERGENCY → [AlertLevel.EMERGENCY]
 *
 * BAN and BANNED_ACCOUNT_DATA are not surfaced as UI alerts on mobile.
 */
internal enum class AlertLevel {
    INFO,
    WARN,
    EMERGENCY,
}

/**
 * Actions the user can trigger from alert UI surfaces.
 */
internal sealed interface SecurityAlertUiAction {
    /**
     * User tapped the dismiss/close button on the alert banner.
     * The presenter should call AlertNotificationsService.dismissAlert() for [alertId],
     * then reveal the next queued alert if any.
     */
    data class DismissAlert(
        val alertId: String,
    ) : SecurityAlertUiAction

    /**
     * User tapped "Update Now" from the version-gate emergency dialog.
     * The presenter should open the app store listing.
     */
    data object OpenAppStore : SecurityAlertUiAction
}

// ---------------------------------------------------------------------------
// Color / icon helpers (internal to this file)
// ---------------------------------------------------------------------------

/**
 * Returns the accent color for the given alert level.
 *
 * INFO:      green  — informational, no immediate action needed.
 * WARN:      amber  — user should be aware; trading continues.
 * EMERGENCY: red    — highest urgency; may block trading.
 */
@Composable
private fun alertAccentColor(level: AlertLevel): Color =
    when (level) {
        AlertLevel.INFO -> BisqTheme.colors.primary
        AlertLevel.WARN -> BisqTheme.colors.warning
        AlertLevel.EMERGENCY -> BisqTheme.colors.danger
    }

/**
 * Returns the subtle background fill for the banner body area.
 *
 * A 12% alpha overlay on the accent color keeps the banner readable without
 * consuming too much visual weight in the app chrome. The left border strip
 * (4 dp wide) provides the strong color signal while the fill stays subtle.
 */
@Composable
private fun alertBannerBackground(level: AlertLevel): Color =
    when (level) {
        AlertLevel.INFO -> BisqTheme.colors.primary.copy(alpha = 0.12f)
        AlertLevel.WARN -> BisqTheme.colors.warning.copy(alpha = 0.12f)
        AlertLevel.EMERGENCY -> BisqTheme.colors.danger.copy(alpha = 0.15f)
    }

/**
 * Material icon vector for each alert level.
 *
 * INFO uses the Info icon — neutral, not alarming.
 * WARN uses the Warning triangle — familiar danger signal.
 * EMERGENCY uses the Warning triangle as well — same shape, different color context.
 *
 * Using Material icons avoids a dependency on project resources in this PoC.
 * The implementation can swap these for project-specific icons from Icons.kt.
 */
private fun alertIcon(level: AlertLevel): ImageVector =
    when (level) {
        AlertLevel.INFO -> Icons.Default.Info
        AlertLevel.WARN -> Icons.Default.Warning
        AlertLevel.EMERGENCY -> Icons.Default.Warning
    }

// ---------------------------------------------------------------------------
// AlertLevelBadge — smallest atom, reusable in lists and notification count
// ---------------------------------------------------------------------------

/**
 * Circular badge showing the alert level with color coding.
 *
 * Used in two places:
 *   1. As the leading icon inside the [SecurityAlertBanner]
 *   2. As a status indicator in any screen that needs to signal an active alert
 *      without showing the full banner (e.g., bottom nav badge).
 *
 * The 28 dp size is intentional: large enough to be clearly visible as a status
 * indicator but small enough not to dominate a row layout.
 *
 * @param level Drives the background color and icon.
 */
@Composable
internal fun AlertLevelBadge(
    level: AlertLevel,
    modifier: Modifier = Modifier,
) {
    val accentColor = alertAccentColor(level)
    Box(
        modifier =
            modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = alertIcon(level),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// PendingAlertsCounter — "more alerts" chip
// ---------------------------------------------------------------------------

/**
 * Small chip shown inside the banner when more than one alert is queued.
 *
 * Design rationale: rather than cycling through all alerts automatically
 * (which would feel intrusive and makes it hard to re-read a message),
 * the counter tells the user "there are N more" and they can dismiss the
 * current alert to see the next one in priority order.
 *
 * The green chip color uses [BisqTheme.colors.primaryDim] to distinguish
 * this count from the alert severity colors.
 *
 * @param count Number of *additional* queued alerts beyond the currently displayed one.
 */
@Composable
internal fun PendingAlertsCounter(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey50)
                .padding(horizontal = BisqUIConstants.ScreenPaddingHalf, vertical = BisqUIConstants.ScreenPaddingQuarter),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.XSmallMedium(
            text = "+$count more",
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

// ---------------------------------------------------------------------------
// SecurityAlertBanner — the primary persistent molecule
// ---------------------------------------------------------------------------

/**
 * Persistent alert banner that sits pinned immediately below the TopBar.
 *
 * ## Layout
 * ```
 * ┌────────────────────────────────────────────────────────────┐
 * │ ▌ [icon] Headline text                        [+N] [close] │
 * │ ▌ Body message wrapping across multiple lines if needed.   │
 * └────────────────────────────────────────────────────────────┘
 *   ↑ 4 dp colored left border strip (accent color)
 * ```
 *
 * ## Dismissibility rules
 * - INFO and WARN alerts: always dismissible (close button visible).
 * - EMERGENCY with haltTrading=false: dismissible.
 * - EMERGENCY with haltTrading=true: NOT dismissible from the banner.
 *   The close button is hidden. The user must acknowledge the blocking modal instead.
 *   Rationale: if trading is halted by the security manager, the user should not be
 *   able to quietly dismiss the warning and accidentally try to open a trade.
 *
 * ## Visibility
 * Wrap in [AnimatedVisibility] at call sites for slide-in/slide-out transitions,
 * matching the desktop's slide-in-right animation behavior.
 *
 * @param alert The current alert data to display.
 * @param pendingCount Number of additional queued alerts to show in the counter chip.
 * @param onDismiss Called when the user taps the close button. Receives the alert ID.
 */
@Composable
internal fun SecurityAlertBanner(
    alert: SecurityAlertUiState,
    onDismiss: (alertId: String) -> Unit,
    modifier: Modifier = Modifier,
    pendingCount: Int = 0,
) {
    val accentColor = alertAccentColor(alert.alertLevel)
    val bannerBackground = alertBannerBackground(alert.alertLevel)
    val isDismissible = !(alert.alertLevel == AlertLevel.EMERGENCY && alert.haltTrading)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(bannerBackground)
                .border(
                    width = 4.dp,
                    color = accentColor,
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp),
                ).testTag("securityAlert_${alert.alertLevel.name}"),
    ) {
        // Left accent strip is implemented via the border above; add left padding to clear it.
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        start = BisqUIConstants.ScreenPadding + 4.dp, // 4 dp = border width
                        end = BisqUIConstants.ScreenPaddingHalf,
                        top = BisqUIConstants.ScreenPadding,
                        bottom = BisqUIConstants.ScreenPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
        ) {
            // Headline row: icon + text + counter chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                AlertLevelBadge(level = alert.alertLevel)
                BisqText.StyledText(
                    text = alert.headline,
                    color = accentColor,
                    style = BisqTheme.typography.smallMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (pendingCount > 0) {
                    PendingAlertsCounter(count = pendingCount)
                }
            }

            // Body message — only rendered if non-blank
            if (alert.message.isNotBlank()) {
                BisqText.SmallLight(
                    text = alert.message,
                    color = BisqTheme.colors.light_grey10,
                )
            }

            // Trading halted notice — only for EMERGENCY + haltTrading
            if (alert.alertLevel == AlertLevel.EMERGENCY && alert.haltTrading) {
                BisqText.XSmallMedium(
                    text = "New trades are suspended until this alert is lifted.",
                    color = BisqTheme.colors.danger,
                )
            }
        }

        // Close button — hidden when alert is non-dismissible (haltTrading + EMERGENCY)
        if (isDismissible) {
            IconButton(
                onClick = { onDismiss(alert.id) },
                modifier =
                    Modifier
                        .align(Alignment.Top)
                        .padding(top = BisqUIConstants.ScreenPaddingQuarter)
                        .testTag("dismissAlert"),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss alert",
                    tint = BisqTheme.colors.mid_grey20,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            // Reserve the same space so the content column doesn't shift
            // Spacer reserves the same width as the close IconButton (48 dp default + padding)
            // so the headline column doesn't shift when the close button is hidden.
            Box(modifier = Modifier.size(width = BisqUIConstants.ScreenPadding4X, height = BisqUIConstants.Zero))
        }
    }
}

// ---------------------------------------------------------------------------
// EmergencyHaltDialog — blocking modal for trading-suspended state
// ---------------------------------------------------------------------------

/**
 * Blocking modal dialog shown when an EMERGENCY alert has haltTrading=true.
 *
 * ## Design rationale — why a separate modal rather than just the banner?
 * When trading is halted by the security manager, ANY attempt to open a trade
 * screen or confirm a trade action must be intercepted. The banner alone is
 * insufficient: the user may already be mid-trade and not see the banner.
 * A modal ensures the blocking condition is communicated before a user can
 * waste time entering trade details they cannot submit.
 *
 * ## Dismissibility
 * The dialog is NOT dismissible by tapping outside (dismissOnClickOutside = false).
 * There is no dismiss button — the only way out is for the security manager to
 * lift the alert. This mirrors the severity: if trading is halted across the
 * network, the user must be aware of it before doing anything.
 *
 * The presenter observes the alert list and removes this dialog when the
 * emergency alert is gone from the network (either expired or security manager
 * published a new alert without haltTrading).
 *
 * ## Accessibility
 * The dialog has a contentDescription on its root so screen readers announce
 * "Emergency: trading suspended" when it appears.
 *
 * @param alert The active EMERGENCY alert with haltTrading=true.
 */
@Composable
internal fun EmergencyHaltDialog(
    alert: SecurityAlertUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .border(
                    width = 1.dp,
                    color = BisqTheme.colors.danger,
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                ).padding(BisqUIConstants.ScreenPadding2X)
                .testTag("emergencyHaltDialog"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            // Large warning icon — immediately communicates severity
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = BisqTheme.colors.danger,
                modifier = Modifier.size(48.dp),
            )

            BisqText.H6Regular(
                text = alert.headline,
                color = BisqTheme.colors.danger,
                textAlign = TextAlign.Center,
            )

            if (alert.message.isNotBlank()) {
                BisqText.BaseLight(
                    text = alert.message,
                    color = BisqTheme.colors.light_grey10,
                    textAlign = TextAlign.Center,
                )
            }

            BisqGap.V1()

            // Non-interactive status pill — not a button, just a status label.
            // There is intentionally no action for the user here: they must wait.
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .background(BisqTheme.colors.danger.copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = BisqTheme.colors.danger.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                        ).padding(
                            horizontal = BisqUIConstants.ScreenPadding,
                            vertical = BisqUIConstants.ScreenPaddingHalf,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                BisqText.SmallMedium(
                    text = "New trades are suspended until this alert is lifted.",
                    color = BisqTheme.colors.danger,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// VersionGateDialog — blocking modal for requiresUpdate state
// ---------------------------------------------------------------------------

/**
 * Modal dialog shown when an EMERGENCY alert enforces a minimum app version.
 *
 * ## Design rationale
 * Unlike the trading halt dialog (which has no user action), the version gate
 * requires the user to take a specific action: open the app store and update.
 * The "Update Now" CTA is therefore the primary affordance — green (primary)
 * to signal that it is the positive path forward, not a destructive action.
 *
 * The message explains WHY the update is needed (trading requires a minimum
 * version) rather than just stating "please update". This builds trust by
 * being transparent about the system's enforcement model.
 *
 * @param alert The EMERGENCY alert with requiresUpdate=true.
 * @param onUpdateNow Called when the user taps "Update Now". Presenter opens the app store.
 */
@Composable
internal fun VersionGateDialog(
    alert: SecurityAlertUiState,
    onUpdateNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .border(
                    width = 1.dp,
                    color = BisqTheme.colors.warning,
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                ).padding(BisqUIConstants.ScreenPadding2X)
                .testTag("versionGateDialog"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = BisqTheme.colors.warning,
                modifier = Modifier.size(48.dp),
            )

            BisqText.H6Regular(
                text = "Update Required",
                color = BisqTheme.colors.warning,
                textAlign = TextAlign.Center,
            )

            val versionText =
                if (alert.minVersion.isNotBlank()) {
                    "Trading requires app version ${alert.minVersion} or newer. Please update to continue trading."
                } else {
                    "A required update is available. Please update to continue trading."
                }
            BisqText.BaseLight(
                text = versionText,
                color = BisqTheme.colors.light_grey10,
                textAlign = TextAlign.Center,
            )

            if (alert.message.isNotBlank()) {
                BisqText.SmallLight(
                    text = alert.message,
                    color = BisqTheme.colors.mid_grey30,
                    textAlign = TextAlign.Center,
                )
            }

            BisqGap.V1()

            BisqButton(
                text = "Update Now",
                onClick = onUpdateNow,
                fullWidth = true,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview
@Composable
private fun AlertLevelBadge_AllLevels_Preview() {
    BisqTheme.Preview {
        Row(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlertLevelBadge(level = AlertLevel.INFO)
            BisqText.SmallLight(text = "Info", color = BisqTheme.colors.primary)
            AlertLevelBadge(level = AlertLevel.WARN)
            BisqText.SmallLight(text = "Warn", color = BisqTheme.colors.warning)
            AlertLevelBadge(level = AlertLevel.EMERGENCY)
            BisqText.SmallLight(text = "Emergency", color = BisqTheme.colors.danger)
        }
    }
}

@Preview
@Composable
private fun SecurityAlertBanner_Info_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor)) {
            SimulatedTopBar(title = "Offerbook")
            AnimatedVisibility(
                visible = true,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SecurityAlertBanner(
                    alert =
                        simulatedAlert(
                            level = AlertLevel.INFO,
                            headline = "Network Information",
                            message = "A routine maintenance window is scheduled for this weekend. Connectivity may be briefly interrupted.",
                        ),
                    pendingCount = 0,
                    onDismiss = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun SecurityAlertBanner_Warn_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor)) {
            SimulatedTopBar(title = "Offerbook")
            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.WARN,
                        headline = "Network Warning",
                        message = "Unusual network conditions detected. Trade carefully and verify payment confirmations before releasing Bitcoin.",
                    ),
                pendingCount = 2,
                onDismiss = {},
            )
        }
    }
}

@Preview
@Composable
private fun SecurityAlertBanner_Emergency_Dismissible_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor)) {
            SimulatedTopBar(title = "Offerbook")
            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.EMERGENCY,
                        headline = "Emergency Alert",
                        message = "A critical issue has been identified. The security team is investigating. Monitor official channels.",
                        haltTrading = false,
                    ),
                pendingCount = 0,
                onDismiss = {},
            )
        }
    }
}

@Preview
@Composable
private fun SecurityAlertBanner_Emergency_HaltTrading_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor)) {
            SimulatedTopBar(title = "Offerbook")
            // No close button when trading is halted
            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.EMERGENCY,
                        headline = "Emergency Alert",
                        message = "A critical security vulnerability has been discovered. Trading is temporarily suspended.",
                        haltTrading = true,
                    ),
                pendingCount = 0,
                onDismiss = {},
            )
        }
    }
}

@Preview
@Composable
private fun PendingAlertsCounter_Preview() {
    BisqTheme.Preview {
        Row(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            PendingAlertsCounter(count = 1)
            PendingAlertsCounter(count = 3)
            PendingAlertsCounter(count = 0) // should render nothing
        }
    }
}

@Preview
@Composable
private fun EmergencyHaltDialog_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            EmergencyHaltDialog(
                alert =
                    simulatedAlert(
                        level = AlertLevel.EMERGENCY,
                        headline = "Trading Suspended",
                        message = "A critical security vulnerability requires immediate action. All trading is temporarily halted until the issue is resolved. We apologize for the interruption.",
                        haltTrading = true,
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun VersionGateDialog_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            VersionGateDialog(
                alert =
                    simulatedAlert(
                        level = AlertLevel.EMERGENCY,
                        headline = "Update Required",
                        message = "This version contains a security fix. Please update immediately.",
                        requiresUpdate = true,
                        minVersion = "2.1.8",
                    ),
                onUpdateNow = {},
            )
        }
    }
}

@Preview
@Composable
private fun VersionGateDialog_NoMinVersion_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            VersionGateDialog(
                alert =
                    simulatedAlert(
                        level = AlertLevel.EMERGENCY,
                        headline = "Update Required",
                        message = "",
                        requiresUpdate = true,
                        minVersion = "",
                    ),
                onUpdateNow = {},
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Simulated preview helpers — no domain types, no Koin
// ---------------------------------------------------------------------------

@Composable
internal fun SimulatedTopBar(title: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.dark_grey30)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.BaseRegular(text = title, color = BisqTheme.colors.white)
    }
}

/**
 * Constructs a [SecurityAlertUiState] with simulated data for previews.
 * Uses a stable ID "preview-alert" to avoid flicker in stateful previews.
 */
internal fun simulatedAlert(
    level: AlertLevel,
    headline: String,
    message: String,
    haltTrading: Boolean = false,
    requiresUpdate: Boolean = false,
    minVersion: String = "",
): SecurityAlertUiState =
    SecurityAlertUiState(
        id = "preview-alert",
        alertLevel = level,
        headline = headline,
        message = message,
        haltTrading = haltTrading,
        requiresUpdate = requiresUpdate,
        minVersion = minVersion,
    )
