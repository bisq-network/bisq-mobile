/**
 * # SecurityAlertScreens.kt — Design PoC (Issue #1128)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 * Full-screen compositions showing how security alerts integrate into the existing
 * screen layouts.
 *
 * ======================================================================================
 * INTEGRATION ARCHITECTURE
 * ======================================================================================
 * The production implementation should surface alerts at two levels:
 *
 * 1. **Persistent banner** — pinned below the TopBar in the main scaffold.
 *    The app's root scaffold (MainScreen / RootLayout) observes an AlertsPresenter
 *    which emits the most-relevant queued alert. When an alert is present, it injects
 *    the [SecurityAlertBanner] composable between the TopBar and the screen content.
 *    This mirrors the desktop's AlertBannerController lifecycle.
 *
 * 2. **Blocking modal** — shown as a Dialog when:
 *    - alertLevel == EMERGENCY && haltTrading == true  → [EmergencyHaltDialog]
 *    - alertLevel == EMERGENCY && requiresUpdate == true → [VersionGateDialog]
 *    Both dialogs use a custom Box layout (not the Dialog composable) to control
 *    dismissibility at the presenter layer rather than at the composable layer.
 *    The presenter shows the dialog when an alert is active and hides it when gone.
 *
 * ======================================================================================
 * SCREEN CONTEXTS SHOWN IN THIS FILE
 * ======================================================================================
 * 1. Offerbook with info banner — baseline scenario, no disruption to trading UX.
 * 2. Offerbook with warning banner + multiple alerts — elevated severity.
 * 3. Offerbook with emergency banner (trading NOT halted) — critical but still tradeable.
 * 4. Offerbook with emergency banner (trading HALTED) — non-dismissible banner + dialog.
 * 5. Trade screen with warning banner — alert visible mid-trade.
 * 6. Version gate screen — blocking update prompt over a screen.
 * 7. No alert (normal state) — the default, for comparison.
 * 8. Alert-only showcase — the three banner types stacked.
 *
 * ======================================================================================
 * DESIGN DECISIONS
 * ======================================================================================
 *
 * ### Banner position: below TopBar, not above content as overlay
 * On desktop, the banner slides in from the right over the top padding area. On mobile,
 * overlaying content is problematic: it may cover CTAs in the active trade step or the
 * offer list filter buttons. Placing the banner BELOW the TopBar but ABOVE the page
 * content ensures it's always visible without obscuring interactive elements.
 *
 * ### Emergency halt: no dialog close button
 * The EmergencyHaltDialog has no dismiss path for the user. This is intentional and
 * matches the severity of a network-wide trading halt. The presenter should clear the
 * dialog ONLY when the security manager lifts the emergency. This prevents a user from
 * accidentally dismissing the warning, then wondering why their trade failed.
 *
 * ### Version gate: positive framing
 * The VersionGateDialog uses the primary green for the "Update Now" CTA rather than
 * danger red. This is deliberate: the user IS NOT being warned of danger in their action —
 * they ARE being invited to take the correct path forward. Red would signal "don't do this".
 *
 * ### Multiple alerts: one at a time, badge for count
 * Desktop shows one alert at a time (the most severe / most recent). We match this.
 * The +N badge in the banner tells users there are more alerts they haven't seen yet,
 * without auto-cycling and interrupting their reading flow.
 *
 * ### Text length safety
 * Headlines: max 1 line with ellipsis. The field on the domain is Optional<String> with
 * max 1000 chars, but headlines should be short — the security manager tool enforces
 * this editorially. We add ellipsis as a defensive measure.
 * Message body: wraps freely, no truncation. Users need to read the full message.
 */
package network.bisq.mobile.presentation.design.security_alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

// ---------------------------------------------------------------------------
// Preview 1 — Normal state (no alerts)
// ---------------------------------------------------------------------------

/**
 * Baseline: the offerbook with no active alerts.
 *
 * Shown here as a reference so reviewers can see the delta when alerts appear.
 * The offerbook content area fills the full height below the TopBar.
 */
@Preview
@Composable
private fun Offerbook_NoAlert_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor),
        ) {
            SimulatedTopBar(title = "Offerbook")
            // No banner here — banner slot is empty
            SimulatedOfferbookContent(offerCount = 8)
        }
    }
}

// ---------------------------------------------------------------------------
// Preview 2 — Info banner on Offerbook
// ---------------------------------------------------------------------------

/**
 * Informational alert on the offerbook.
 *
 * The green banner is subtle — informational alerts do not indicate danger, so
 * they should not alarm users browsing offers. The green color is consistent
 * with the Bisq brand (primary color) to signal "network message", not "danger".
 *
 * The banner can be dismissed. After dismissal the full offer list height is restored.
 */
@Preview
@Composable
private fun Offerbook_InfoBanner_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor),
        ) {
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
                            message = "A routine maintenance window is scheduled. Connectivity may be briefly interrupted this weekend.",
                        ),
                    pendingCount = 0,
                    onDismiss = {},
                )
            }
            SimulatedOfferbookContent(offerCount = 8)
        }
    }
}

// ---------------------------------------------------------------------------
// Preview 3 — Warning banner with multiple pending alerts
// ---------------------------------------------------------------------------

/**
 * Warning alert on the offerbook with 2 additional queued alerts.
 *
 * The amber banner draws more attention than an info banner. The "+2 more" chip
 * signals that the user has not yet seen all queued messages — dismissing this
 * one will reveal the next in priority order.
 *
 * The amber color is distinct from both green (info/success) and red (danger),
 * creating a clear three-tier visual hierarchy: green < amber < red.
 */
@Preview
@Composable
private fun Offerbook_WarnBanner_MultipleAlerts_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor),
        ) {
            SimulatedTopBar(title = "Offerbook")
            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.WARN,
                        headline = "Network Warning",
                        message = "Unusual network conditions detected. Please verify payment confirmations before releasing Bitcoin.",
                    ),
                pendingCount = 2,
                onDismiss = {},
            )
            SimulatedOfferbookContent(offerCount = 6)
        }
    }
}

// ---------------------------------------------------------------------------
// Preview 4 — Emergency banner, trading NOT halted (still dismissible)
// ---------------------------------------------------------------------------

/**
 * Emergency alert without trading halt.
 *
 * The security manager has issued an EMERGENCY-level message but has not
 * activated the trading halt flag. Trading continues, but users are strongly
 * advised to read the message. The red banner is alarming by design.
 *
 * The close button is still present — the user can dismiss after reading.
 * The presenter is responsible for re-showing if the user navigates away and
 * comes back (per the desktop pattern: alert stays until explicitly dismissed).
 */
@Preview
@Composable
private fun Offerbook_EmergencyBanner_TradingAllowed_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor),
        ) {
            SimulatedTopBar(title = "Offerbook")
            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.EMERGENCY,
                        headline = "Emergency Alert",
                        message = "A critical issue has been identified. The security team is investigating. Monitor official Bisq channels for updates.",
                        haltTrading = false,
                    ),
                pendingCount = 0,
                onDismiss = {},
            )
            SimulatedOfferbookContent(offerCount = 4)
        }
    }
}

// ---------------------------------------------------------------------------
// Preview 5 — Emergency banner + halt dialog (trading halted)
// ---------------------------------------------------------------------------

/**
 * Emergency alert with trading halted.
 *
 * This is the most critical state. Two simultaneous UI elements are shown:
 *   1. The non-dismissible banner — pinned below TopBar, no close button.
 *   2. The EmergencyHaltDialog — overlaid over the offerbook content.
 *
 * The offerbook content is still rendered beneath the dialog (not removed) so
 * that when the alert is eventually lifted, the screen snaps back to normal
 * without a navigation event or data reload.
 *
 * The dialog is NOT inside a Dialog/AlertDialog composable because we need to
 * control its dismissibility purely from the presenter. Instead it is a regular
 * Box that is conditionally visible via AnimatedVisibility.
 *
 * Accessibility: the content behind the dialog is NOT inert in this PoC — the
 * implementation should set `Modifier.semantics { disabled() }` on the background
 * content while the blocking dialog is active.
 */
@Preview
@Composable
private fun Offerbook_EmergencyBanner_TradingHalted_Preview() {
    BisqTheme.Preview {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SimulatedTopBar(title = "Offerbook")
                // Non-dismissible banner — no close button
                SecurityAlertBanner(
                    alert =
                        simulatedAlert(
                            level = AlertLevel.EMERGENCY,
                            headline = "Emergency Alert",
                            message = "A critical security vulnerability has been discovered.",
                            haltTrading = true,
                        ),
                    pendingCount = 0,
                    onDismiss = {},
                )
                // Offer list is still rendered but visually behind the dialog
                SimulatedOfferbookContent(offerCount = 3)
            }

            // Scrim behind the dialog
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(BisqTheme.colors.backgroundColor.copy(alpha = 0.75f)),
            )

            // Blocking dialog centred on screen
            EmergencyHaltDialog(
                alert =
                    simulatedAlert(
                        level = AlertLevel.EMERGENCY,
                        headline = "Trading Suspended",
                        message = "All new trades are temporarily halted while a critical security issue is being resolved. Existing trades are unaffected. The Bisq security team is working to resolve this as quickly as possible.",
                        haltTrading = true,
                    ),
                modifier =
                    Modifier
                        .padding(BisqUIConstants.ScreenPadding2X)
                        .align(Alignment.Center),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview 6 — Warning banner during an active trade
// ---------------------------------------------------------------------------

/**
 * Warning alert during an active trade.
 *
 * Demonstrates the most critical integration point: an alert arriving while
 * the user is in the middle of a trade. The banner must not disrupt the trade
 * flow — it sits above the trade content with the standard banner molecule.
 *
 * Key UX decision: the trade step indicator and trade chat must remain fully
 * accessible even with the banner present. The banner compresses the trade
 * content vertically, which is acceptable (the content scrolls). It does NOT
 * overlay any buttons or CTAs.
 *
 * The trade-chat input is still at the bottom — the banner only affects the
 * top content area height.
 */
@Preview
@Composable
private fun TradeChatScreen_WithWarnBanner_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor),
        ) {
            SimulatedTopBar(title = "Trade · 0.01 BTC / 350 EUR")
            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.WARN,
                        headline = "Network Warning",
                        message = "Unusual conditions detected. Verify payment confirmations carefully.",
                    ),
                pendingCount = 0,
                onDismiss = {},
            )
            // Trade step header
            SimulatedTradeStepHeader(step = "Waiting for buyer's payment")
            // Trade chat (simulated)
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(BisqTheme.colors.backgroundColor)
                        .padding(BisqUIConstants.ScreenPadding),
                contentAlignment = Alignment.BottomStart,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)) {
                    SimulatedSystemMessage("Trade contract signed. Waiting for buyer to initiate payment.")
                    SimulatedOutboundBubble("I have sent the payment via SEPA.")
                    SimulatedInboundBubble("Thanks, I will check and confirm when received.")
                }
            }
            SimulatedChatInputArea()
        }
    }
}

// ---------------------------------------------------------------------------
// Preview 7 — Version gate dialog
// ---------------------------------------------------------------------------

/**
 * Version gate: the user's app is below the minimum required version.
 *
 * The screen behind the dialog shows the normal offerbook, but the user cannot
 * interact with the trade flow until they update. The "Update Now" CTA opens the
 * platform app store.
 *
 * Unlike the trading halt dialog, this one HAS a positive exit path for the user:
 * tapping "Update Now" resolves the situation. This is why the button uses the
 * primary green — it signals the correct forward action.
 */
@Preview
@Composable
private fun Offerbook_VersionGate_Preview() {
    BisqTheme.Preview {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SimulatedTopBar(title = "Offerbook")
                SecurityAlertBanner(
                    alert =
                        simulatedAlert(
                            level = AlertLevel.EMERGENCY,
                            headline = "Emergency Alert",
                            message = "Please update the app to continue trading.",
                            requiresUpdate = true,
                            minVersion = "2.1.8",
                        ),
                    pendingCount = 0,
                    onDismiss = {},
                )
                SimulatedOfferbookContent(offerCount = 3)
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(BisqTheme.colors.backgroundColor.copy(alpha = 0.75f)),
            )

            VersionGateDialog(
                alert =
                    simulatedAlert(
                        level = AlertLevel.EMERGENCY,
                        headline = "Update Required",
                        message = "This version contains a critical security fix. The network requires you to update before trading.",
                        requiresUpdate = true,
                        minVersion = "2.1.8",
                    ),
                onUpdateNow = {},
                modifier =
                    Modifier
                        .padding(BisqUIConstants.ScreenPadding2X)
                        .align(Alignment.Center),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview 8 — All three banner severity levels stacked (showcase)
// ---------------------------------------------------------------------------

/**
 * Design showcase: all three severity levels stacked vertically.
 *
 * Not a real app state — used to compare the three levels side-by-side
 * in the Android Studio Preview panel. Useful during design review to
 * ensure the color differentiation is clear at a glance.
 */
@Preview
@Composable
private fun AllAlertBanners_Showcase_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallRegular(
                text = "Alert severity levels (left-border color coding)",
                color = BisqTheme.colors.mid_grey20,
            )
            BisqGap.VHalf()

            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.INFO,
                        headline = "Network Information",
                        message = "Maintenance window scheduled this weekend.",
                    ),
                onDismiss = {},
            )
            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.WARN,
                        headline = "Network Warning",
                        message = "Unusual conditions detected. Trade carefully.",
                    ),
                pendingCount = 1,
                onDismiss = {},
            )
            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.EMERGENCY,
                        headline = "Emergency Alert",
                        message = "Critical issue identified. Monitor official channels.",
                        haltTrading = false,
                    ),
                onDismiss = {},
            )
            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.EMERGENCY,
                        headline = "Emergency Alert — Trading Halted",
                        message = "All new trades are suspended.",
                        haltTrading = true,
                    ),
                onDismiss = {},
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview 9 — Long headline and long message text (text expansion stress test)
// ---------------------------------------------------------------------------

/**
 * Stress test: simulates a very long headline and a multi-paragraph body message.
 *
 * The headline is truncated at 1 line with ellipsis — confirming the banner does
 * not grow unboundedly if the security manager sends a verbose headline.
 * The message wraps freely, which is intentional — users need to read the full text.
 *
 * This also simulates the German/Russian text expansion scenario (~35% longer than EN).
 */
@Preview
@Composable
private fun SecurityAlertBanner_LongText_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.background(BisqTheme.colors.backgroundColor),
        ) {
            SimulatedTopBar(title = "Offerbook")
            SecurityAlertBanner(
                alert =
                    simulatedAlert(
                        level = AlertLevel.WARN,
                        headline = "Netzwerkwarnung: Ungewöhnliche Netzwerkbedingungen erkannt — bitte vorsichtig handeln",
                        message =
                            "Es wurden ungewöhnliche Netzwerkbedingungen festgestellt. " +
                                "Bitte überprüfen Sie alle Zahlungsbestätigungen sorgfältig, " +
                                "bevor Sie Bitcoin freigeben. Das Sicherheitsteam untersucht " +
                                "die Situation und wird so bald wie möglich ein Update bereitstellen.",
                    ),
                pendingCount = 3,
                onDismiss = {},
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Simulated content helpers (no domain types, no Koin)
// ---------------------------------------------------------------------------

@Composable
private fun SimulatedOfferbookContent(offerCount: Int) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        item { BisqGap.V1() }
        items(offerCount) { index ->
            SimulatedOfferRow(
                amount = "${(index + 1) * 50} EUR",
                price = "${27_000 + index * 100} EUR/BTC",
                seller = "Trader${(index + 1) * 7}#${1000 + index * 33}",
                stars = 4.0 + (index % 3) * 0.5,
            )
        }
        item { BisqGap.V1() }
    }
}

@Composable
private fun SimulatedOfferRow(
    amount: String,
    price: String,
    seller: String,
    stars: Double,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(BisqUIConstants.ScreenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
            BisqText.BaseRegular(text = amount, color = BisqTheme.colors.white)
            BisqText.SmallLight(text = price, color = BisqTheme.colors.mid_grey30)
            BisqText.XSmallRegular(text = seller, color = BisqTheme.colors.mid_grey20)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            SimulatedStarRow(stars = stars)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = BisqTheme.colors.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SimulatedStarRow(stars: Double) {
    val fullStars = stars.toInt()
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { i ->
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < fullStars) BisqTheme.colors.primary else BisqTheme.colors.mid_grey10,
                        ),
            )
        }
    }
}

@Composable
private fun SimulatedTradeStepHeader(step: String) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.dark_grey30)
                    .padding(BisqUIConstants.ScreenPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                BisqText.SmallRegular(text = "Step 3 of 4", color = BisqTheme.colors.mid_grey20)
                BisqText.BaseRegular(text = step, color = BisqTheme.colors.white)
            }
            BisqButton(
                text = "Confirm",
                onClick = {},
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = BisqTheme.colors.dark_grey50)
    }
}

@Composable
private fun SimulatedSystemMessage(text: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey30)
                .padding(BisqUIConstants.ScreenPadding),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.SmallLight(
            text = text,
            color = BisqTheme.colors.mid_grey20,
        )
    }
}

@Composable
private fun SimulatedInboundBubble(text: String) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(BisqTheme.colors.dark_grey50),
        )
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BisqTheme.colors.dark_grey40)
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(BisqTheme.colors.primaryDisabled)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.BaseLight(text = text, color = BisqTheme.colors.white)
        }
    }
}

@Composable
private fun SimulatedChatInputArea() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.secondary)
                .padding(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(BisqUIConstants.textFieldBorderRadius))
                    .background(BisqTheme.colors.dark_grey40)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.BaseLight(text = "Write a message...", color = BisqTheme.colors.mid_grey20)
        }
    }
}
