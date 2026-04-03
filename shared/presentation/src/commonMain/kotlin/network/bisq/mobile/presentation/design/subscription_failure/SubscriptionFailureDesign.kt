package network.bisq.mobile.presentation.design.subscription_failure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIconFilled
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.ui.tooling.preview.Preview

// ─────────────────────────────────────────────
// Data model (primitive-only — no domain types)
// ─────────────────────────────────────────────

/**
 * Whether a failed subscription is critical (breaks core functionality) or cosmetic
 * (only affects minor display elements).
 *
 * Critical: MARKET_PRICE, OFFERS, TRADES, TRADE_PROPERTIES, TRADE_CHAT_MESSAGES, REPUTATION
 * Cosmetic:  NUM_OFFERS, CHAT_REACTIONS, NUM_USER_PROFILES
 */
private enum class FailedTopicSeverity { CRITICAL, COSMETIC }

/**
 * A single failed subscription expressed in user-facing language.
 *
 * @param featureName  Short name shown in the list row (e.g. "Live price").
 * @param impact       One-line description of what the user loses (e.g. "Prices will not update.").
 * @param severity     CRITICAL or COSMETIC — drives icon and color treatment.
 */
private data class SimulatedFailedTopic(
    val featureName: String,
    val impact: String,
    val severity: FailedTopicSeverity,
)

// ─────────────────────────────────────────────
// Atoms
// ─────────────────────────────────────────────

/**
 * A single row in the failed-topic list.
 *
 * Critical topics get the amber WarningIcon; cosmetic topics get the grey InfoGreenIcon
 * (intentionally muted so they do not compete visually with critical rows).
 */
@Composable
private fun FailedTopicRow(topic: SimulatedFailedTopic) {
    val iconTint: Color
    val impactColor: Color

    when (topic.severity) {
        FailedTopicSeverity.CRITICAL -> {
            iconTint = BisqTheme.colors.warning
            impactColor = BisqTheme.colors.mid_grey30
        }
        FailedTopicSeverity.COSMETIC -> {
            iconTint = BisqTheme.colors.mid_grey20
            impactColor = BisqTheme.colors.mid_grey20
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        // Icon column — fixed width so all text aligns
        when (topic.severity) {
            FailedTopicSeverity.CRITICAL ->
                WarningIcon(modifier = Modifier.size(18.dp).padding(top = 1.dp))
            FailedTopicSeverity.COSMETIC ->
                InfoGreenIcon(modifier = Modifier.size(18.dp).padding(top = 1.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            BisqText.SmallMedium(
                text = topic.featureName,
                color = BisqTheme.colors.white,
            )
            BisqText.XSmallLight(
                text = topic.impact,
                color = impactColor,
            )
        }
    }
}

/**
 * Section header used to separate critical and cosmetic failures when both are present.
 */
@Composable
private fun SectionLabel(
    text: String,
    color: Color,
) {
    BisqText.XSmallMedium(
        text = text.uppercase(),
        color = color,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * The scrollable body of the dialog listing all failed topics, grouped by severity when mixed.
 *
 * When only one severity level is present the section header is suppressed to reduce clutter.
 */
@Composable
private fun FailedTopicList(topics: List<SimulatedFailedTopic>) {
    val criticalTopics = topics.filter { it.severity == FailedTopicSeverity.CRITICAL }
    val cosmeticTopics = topics.filter { it.severity == FailedTopicSeverity.COSMETIC }
    val hasBothSeverities = criticalTopics.isNotEmpty() && cosmeticTopics.isNotEmpty()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                .background(BisqTheme.colors.dark_grey50)
                .padding(BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        if (hasBothSeverities) {
            SectionLabel(
                text = "Affects core features",
                color = BisqTheme.colors.warning,
            )
        }

        criticalTopics.forEachIndexed { index, topic ->
            FailedTopicRow(topic)
            if (index < criticalTopics.lastIndex) {
                BisqGap.VQuarter()
            }
        }

        if (hasBothSeverities) {
            BisqGap.VHalf()
            SectionLabel(
                text = "Minor display issues only",
                color = BisqTheme.colors.mid_grey20,
            )
            cosmeticTopics.forEachIndexed { index, topic ->
                FailedTopicRow(topic)
                if (index < cosmeticTopics.lastIndex) {
                    BisqGap.VQuarter()
                }
            }
        } else {
            cosmeticTopics.forEachIndexed { index, topic ->
                FailedTopicRow(topic)
                if (index < cosmeticTopics.lastIndex) {
                    BisqGap.VQuarter()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Molecules
// ─────────────────────────────────────────────

/**
 * The headline icon + title row.  Uses the filled warning icon for critical-only scenarios
 * and the regular warning icon when cosmetic items are present (same visual weight, cleaner).
 */
@Composable
private fun DialogHeadline(hasCritical: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        if (hasCritical) {
            WarningIconFilled(modifier = Modifier.size(24.dp))
        } else {
            WarningIcon(modifier = Modifier.size(24.dp))
        }
        BisqText.H6Regular(
            text = "Some features could not start",
            color = if (hasCritical) BisqTheme.colors.warning else BisqTheme.colors.white,
        )
    }
}

/**
 * The contextual subtitle beneath the headline.
 *
 * For critical failures the message is explicit about the risk and names retry as the
 * recommended path.  For cosmetic-only failures the tone is much softer.
 */
@Composable
private fun DialogSubtitle(
    hasCritical: Boolean,
    topicCount: Int,
) {
    val text =
        if (hasCritical) {
            "Your connection succeeded but $topicCount feature${if (topicCount > 1) "s" else ""} " +
                "could not load. You can retry — this often resolves on its own — or continue " +
                "with limited functionality."
        } else {
            "$topicCount minor display feature${if (topicCount > 1) "s are" else " is"} temporarily " +
                "unavailable. Your trading experience is not affected."
        }
    BisqText.BaseLight(text = text, color = BisqTheme.colors.mid_grey30)
}

// ─────────────────────────────────────────────
// Organism: the full dialog
// ─────────────────────────────────────────────

/**
 * Non-dismissible subscription failure dialog.
 *
 * Designed to appear during or immediately after the bootstrap/splash phase.
 *
 * The dialog is non-dismissible (no tap-outside, no system-back dismiss). The user must
 * explicitly choose Retry or Continue.  This is intentional: if critical subscriptions
 * are silently missed, the user may attempt a trade they cannot see or complete.
 *
 * @param failedTopics          List of topics that failed, expressed as user-facing strings.
 * @param isRetrying            When true, the Retry button shows a loading indicator.
 * @param onRetry               Called when the user taps Retry.
 * @param onContinueAnyway      Called when the user taps Continue anyway.
 */
@Composable
private fun SimulatedSubscriptionFailureDialog(
    failedTopics: List<SimulatedFailedTopic>,
    isRetrying: Boolean,
    onRetry: () -> Unit,
    onContinueAnyway: () -> Unit,
) {
    val hasCritical = failedTopics.any { it.severity == FailedTopicSeverity.CRITICAL }

    BisqDialog(
        onDismissRequest = {},
        dismissOnClickOutside = false,
        marginTop = BisqUIConstants.ScreenPadding4X,
    ) {
        // ── Header ──
        DialogHeadline(hasCritical = hasCritical)
        BisqGap.V1()

        // ── Subtitle ──
        DialogSubtitle(hasCritical = hasCritical, topicCount = failedTopics.size)
        BisqGap.V2()

        // ── Failed topic list ──
        FailedTopicList(topics = failedTopics)
        BisqGap.V2()

        // ── Actions ──
        // Retry is the primary CTA (green).  Continue anyway is Grey — it should feel like a
        // deliberate downgrade, not an equally-weighted option.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            BisqButton(
                text = if (isRetrying) "Retrying..." else "Retry",
                isLoading = isRetrying,
                disabled = isRetrying,
                onClick = onRetry,
                fullWidth = true,
            )

            // The "Continue anyway" label is intentionally specific about the trade-off for
            // critical failures, and softer for cosmetic-only failures.
            val continueLabel = if (hasCritical) "Continue with limited features" else "Continue"
            BisqButton(
                text = continueLabel,
                type = BisqButtonType.Grey,
                onClick = onContinueAnyway,
                fullWidth = true,
                disabled = isRetrying,
            )
        }

        // ── Footer hint (critical only) ──
        if (hasCritical) {
            BisqGap.V1()
            BisqText.XSmallLight(
                text = "You can retry again from Settings if the issue persists.",
                color = BisqTheme.colors.mid_grey20,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────
// Simulated data helpers
// ─────────────────────────────────────────────

private fun singleCriticalFailure(): List<SimulatedFailedTopic> =
    listOf(
        SimulatedFailedTopic(
            featureName = "Live Bitcoin price",
            impact = "Prices will not update. Offer amounts shown in your currency may be stale.",
            severity = FailedTopicSeverity.CRITICAL,
        ),
    )

private fun mixedFailures(): List<SimulatedFailedTopic> =
    listOf(
        SimulatedFailedTopic(
            featureName = "Live Bitcoin price",
            impact = "Prices will not update. Offer amounts shown in your currency may be stale.",
            severity = FailedTopicSeverity.CRITICAL,
        ),
        SimulatedFailedTopic(
            featureName = "Trade activity",
            impact = "Open trades will not reflect the latest state until reconnected.",
            severity = FailedTopicSeverity.CRITICAL,
        ),
        SimulatedFailedTopic(
            featureName = "Offer count badges",
            impact = "Market badges will show no count. Browsing is not affected.",
            severity = FailedTopicSeverity.COSMETIC,
        ),
    )

private fun allCriticalFailures(): List<SimulatedFailedTopic> =
    listOf(
        SimulatedFailedTopic(
            featureName = "Live Bitcoin price",
            impact = "Prices will not update. Offer amounts shown in your currency may be stale.",
            severity = FailedTopicSeverity.CRITICAL,
        ),
        SimulatedFailedTopic(
            featureName = "Available offers",
            impact = "The offerbook cannot load. You will not see offers to buy or sell.",
            severity = FailedTopicSeverity.CRITICAL,
        ),
        SimulatedFailedTopic(
            featureName = "Trade activity",
            impact = "Open trades will not reflect the latest state until reconnected.",
            severity = FailedTopicSeverity.CRITICAL,
        ),
        SimulatedFailedTopic(
            featureName = "Trade state updates",
            impact = "The trade flow cannot advance. Sending payment or confirming receipt may fail.",
            severity = FailedTopicSeverity.CRITICAL,
        ),
        SimulatedFailedTopic(
            featureName = "Trade chat",
            impact = "You will not receive new messages from your trade partner.",
            severity = FailedTopicSeverity.CRITICAL,
        ),
        SimulatedFailedTopic(
            featureName = "Reputation scores",
            impact = "Counterparty trust indicators will not load. Trade at your own risk.",
            severity = FailedTopicSeverity.CRITICAL,
        ),
    )

// ─────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────

@ExcludeFromCoverage
@Preview
@Composable
private fun Preview_SingleCriticalFailurePreview() {
    BisqTheme.Preview {
        // Simulated splash screen backdrop so the dialog is seen in context
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.XSmallLight(
                text = "Preview: single critical failure (MARKET_PRICE)",
                color = BisqTheme.colors.mid_grey20,
            )
        }
        SimulatedSubscriptionFailureDialog(
            failedTopics = singleCriticalFailure(),
            isRetrying = false,
            onRetry = {},
            onContinueAnyway = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun Preview_MixedFailures_TwoCriticalOnCosmeticPreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.XSmallLight(
                text = "Preview: 2 critical + 1 cosmetic failure",
                color = BisqTheme.colors.mid_grey20,
            )
        }
        SimulatedSubscriptionFailureDialog(
            failedTopics = mixedFailures(),
            isRetrying = false,
            onRetry = {},
            onContinueAnyway = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun Preview_AllCriticalFailures_WorstCasePreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.XSmallLight(
                text = "Preview: all 6 critical topics down (worst case)",
                color = BisqTheme.colors.mid_grey20,
            )
        }
        SimulatedSubscriptionFailureDialog(
            failedTopics = allCriticalFailures(),
            isRetrying = false,
            onRetry = {},
            onContinueAnyway = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun Preview_RetryingStatePreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.XSmallLight(
                text = "Preview: retry in progress (loading state)",
                color = BisqTheme.colors.mid_grey20,
            )
        }
        SimulatedSubscriptionFailureDialog(
            failedTopics = mixedFailures(),
            isRetrying = true,
            onRetry = {},
            onContinueAnyway = {},
        )
    }
}
