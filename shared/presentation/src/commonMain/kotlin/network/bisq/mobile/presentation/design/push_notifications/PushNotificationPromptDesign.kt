package network.bisq.mobile.presentation.design.push_notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Design POC: Push notification opt-in prompts.
 *
 * Two prompt scenarios:
 *
 * 1. Contextual prompt: shown once when user has an active trade and backgrounds the app.
 *    Uses ConfirmationDialog with vertical buttons. Lead with benefit, then privacy
 *    reassurance, then reversibility.
 *
 * 2. Enhanced initial permission prompt: when the app first asks for OS notification
 *    permission, it can additionally offer the FCM relay opt-in. This replaces the
 *    existing NotificationPermissionDialog for Android only.
 *
 * Both prompts are shown at most once. The primary control surface is Settings.
 */

@Composable
private fun SimulatedRelayOptInPrompt(
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        headline = "Stay informed when the app is closed",
        headlineColor = BisqTheme.colors.white,
        message =
            "Get notified about trade updates even when Bisq is not running.\n\n" +
                "Your notification content is encrypted \u2014 Google only sees " +
                "an opaque payload, not your trade details.\n\n" +
                "You can change this at any time in Settings.",
        confirmButtonText = "Enable notifications",
        dismissButtonText = "Not now",
        verticalButtonPlacement = true,
        onConfirm = onEnable,
        onDismiss = { onDismiss() },
    )
}

@Composable
private fun SimulatedInitialPermissionWithRelay(
    onGrantPermission: () -> Unit,
    onEnableRelay: () -> Unit,
    onSkip: () -> Unit,
) {
    ConfirmationDialog(
        headline = "Enable trade notifications",
        headlineColor = BisqTheme.colors.white,
        message =
            "Bisq needs notification permission to alert you about trade updates.\n\n" +
                "You can also enable relayed notifications to receive alerts even " +
                "when the app is fully closed. Your notification content is " +
                "end-to-end encrypted.",
        confirmButtonText = "Grant permission",
        dismissButtonText = "Don\u2019t ask again",
        verticalButtonPlacement = true,
        onConfirm = onGrantPermission,
        onDismiss = { onSkip() },
    )
}

@Composable
private fun SimulatedTradeContextScreen(
    tradeId: String,
    showPrompt: Boolean,
    onEnableRelay: () -> Unit,
    onDismissPrompt: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor)
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H4Light("Open Trade")
        BisqGap.VHalf()
        BisqText.BaseLight("Trade ID: $tradeId")
        BisqGap.VHalf()
        BisqText.SmallLight(
            text = "Waiting for seller\u2019s payment confirmation\u2026",
            color = BisqTheme.colors.mid_grey20,
        )
        BisqGap.V2()
        BisqText.XSmallMedium(
            text = "[ User backgrounds the app \u2192 prompt appears ]",
            color = BisqTheme.colors.mid_grey30,
        )
    }

    if (showPrompt) {
        SimulatedRelayOptInPrompt(
            onEnable = onEnableRelay,
            onDismiss = onDismissPrompt,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun RelayOptInPrompt_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Contextual prompt (shown once at first active trade):",
                color = BisqTheme.colors.light_grey10,
            )
        }
        SimulatedRelayOptInPrompt(
            onEnable = {},
            onDismiss = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun InitialPermissionWithRelay_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Enhanced initial permission prompt (Android):",
                color = BisqTheme.colors.light_grey10,
            )
        }
        SimulatedInitialPermissionWithRelay(
            onGrantPermission = {},
            onEnableRelay = {},
            onSkip = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeContext_WithPrompt_Preview() {
    BisqTheme.Preview {
        SimulatedTradeContextScreen(
            tradeId = "abc-123-def",
            showPrompt = true,
            onEnableRelay = {},
            onDismissPrompt = {},
        )
    }
}
