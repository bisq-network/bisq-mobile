package network.bisq.mobile.client.trusted_node_setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.TopicImportance
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.i18n.i18n
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

/**
 * Non-dismissible subscription failure dialog.
 *
 * The dialog is non-dismissible (no tap-outside, no system-back dismiss). The user must
 * explicitly choose Retry or Continue.  This is intentional: if critical subscriptions
 * are silently missed, the user may attempt a trade they cannot see or complete.
 */
@Composable
internal fun SubscriptionsFailedDialog(
    state: SubscriptionsFailedDialogUiState,
    onAction: (SubscriptionsFailedDialogUiAction) -> Unit,
) {
    BisqDialog(
        onDismissRequest = {},
        dismissOnClickOutside = false,
        marginTop = BisqUIConstants.ScreenPadding4X,
    ) {
        // ── Header ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            WarningIconFilled(modifier = Modifier.size(24.dp))
            BisqText.H6Regular(
                text = "mobile.client.dialog.failed_subs.title".i18n(),
                color = BisqTheme.colors.warning,
            )
        }
        BisqGap.V1()

        // ── Subtitle ──
        BisqText.BaseLight(
            text = "mobile.client.dialog.failed_subs.message".i18n(),
            color = BisqTheme.colors.mid_grey30,
        )
        BisqGap.V2()

        // ── Failed topic list ──
        FailedTopicList(topics = state.failedTopics)

        // ── Version mismatch hint (shown only when node API differs from the client's expected version) ──
        if (state.connectedApiVersion != null &&
            state.connectedApiVersion != BuildConfig.BISQ_API_VERSION
        ) {
            BisqGap.V1()
            BisqText.XSmallLight(
                text =
                    "mobile.client.dialog.failed_subs.version.mismatch".i18n(
                        state.connectedApiVersion,
                        BuildConfig.BISQ_API_VERSION,
                    ),
                color = BisqTheme.colors.mid_grey20,
            )
        }
        BisqGap.V2()

        // ── Actions ──
        // Retry is the primary CTA (green).  Continue anyway is Grey — it should feel like a
        // deliberate downgrade, not an equally-weighted option.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            BisqButton(
                text = "mobile.action.retry".i18n(),
                onClick = { onAction(SubscriptionsFailedDialogUiAction.OnRetryPress) },
                fullWidth = true,
            )
            BisqButton(
                text = "mobile.client.dialog.failed_subs.continue".i18n(),
                type = BisqButtonType.Grey,
                onClick = { onAction(SubscriptionsFailedDialogUiAction.OnContinuePress) },
                fullWidth = true,
            )
        }
    }
}

/**
 * The body of the dialog listing all failed topics, grouped by severity.
 * The dialog wrapper is responsible for proper scrolling.
 */
@Composable
private fun FailedTopicList(topics: List<Topic>) {
    val criticalTopics = topics.filter { it.importance == TopicImportance.CRITICAL }
    val cosmeticTopics = topics.filter { it.importance == TopicImportance.COSMETIC }
    val hasBothSeverities = criticalTopics.isNotEmpty() && cosmeticTopics.isNotEmpty()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        if (criticalTopics.isNotEmpty()) {
            SectionLabel(
                text = "mobile.client.dialog.failed_subs.core.title".i18n(),
                color = BisqTheme.colors.warning,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                criticalTopics.forEach { topic ->
                    key(topic.name) {
                        FailedTopicRow(topic)
                    }
                }
            }
        }

        if (hasBothSeverities) {
            BisqGap.VHalf()
        }
        if (cosmeticTopics.isNotEmpty()) {
            SectionLabel(
                text = "mobile.client.dialog.failed_subs.minor.title".i18n(),
                color = BisqTheme.colors.light_grey40,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                cosmeticTopics.forEach { topic ->
                    key(topic.name) {
                        FailedTopicRow(topic)
                    }
                }
            }
        }
    }
}

@Composable
private fun FailedTopicRow(topic: Topic) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        when (topic.importance) {
            TopicImportance.CRITICAL ->
                WarningIcon(modifier = Modifier.size(18.dp).padding(top = 1.dp))

            TopicImportance.COSMETIC ->
                InfoGreenIcon(modifier = Modifier.size(18.dp).padding(top = 1.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            BisqText.SmallMedium(
                text = topic.titleKey.i18n(),
                color = BisqTheme.colors.mid_grey30,
            )
            BisqText.XSmallLight(
                text = topic.descriptionKey.i18n(),
                color = BisqTheme.colors.mid_grey30,
            )
        }
    }
}

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

// ─────────────────────────────────────────────
// preview data helpers
// ─────────────────────────────────────────────

@ExcludeFromCoverage
private fun singleCriticalFailure() =
    SubscriptionsFailedDialogUiState(
        listOf(
            Topic.MARKET_PRICE,
        ),
    )

@ExcludeFromCoverage
private fun mixedFailures() =
    SubscriptionsFailedDialogUiState(
        listOf(
            Topic.MARKET_PRICE,
            Topic.TRADE_PROPERTIES,
            Topic.NUM_OFFERS,
        ),
    )

@ExcludeFromCoverage
private fun allCriticalFailures() =
    SubscriptionsFailedDialogUiState(
        Topic.entries.filter
            { it.importance == TopicImportance.CRITICAL },
    )

@ExcludeFromCoverage
private fun singleCriticalFailureWithVersionMismatch() =
    SubscriptionsFailedDialogUiState(
        failedTopics = listOf(Topic.MARKET_PRICE),
        connectedApiVersion = "2.1.0",
    )

// ─────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────

@ExcludeFromCoverage
@Preview
@Composable
private fun SingleCriticalFailurePreview() {
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
        SubscriptionsFailedDialog(
            state = singleCriticalFailure(),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun MixedFailuresTwoCriticalOnCosmeticPreview() {
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
        SubscriptionsFailedDialog(
            state = mixedFailures(),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun VersionMismatchHintPreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.XSmallLight(
                text = "Preview: version mismatch hint (node 2.1.0, expected ${BuildConfig.BISQ_API_VERSION})",
                color = BisqTheme.colors.mid_grey20,
            )
        }
        SubscriptionsFailedDialog(
            state = singleCriticalFailureWithVersionMismatch(),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun AllCriticalFailuresWorstCasePreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.XSmallLight(
                text = "Preview: all critical topics down (worst case)",
                color = BisqTheme.colors.mid_grey20,
            )
        }
        SubscriptionsFailedDialog(
            state = allCriticalFailures(),
            onAction = {},
        )
    }
}
