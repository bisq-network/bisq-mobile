package network.bisq.mobile.presentation.design.push_notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Design POC: Push notification relay opt-in prompts (both platforms).
 *
 * Contextual prompt: shown once when user has an active trade and backgrounds the app.
 * Uses ConfirmationDialog with vertical buttons. Lead with benefit, then privacy
 * reassurance (platform-specific: Google for Android, Apple for iOS), then reversibility.
 *
 * Shown at most once. The primary control surface is Settings.
 */

@Composable
private fun SimulatedRelayOptInPrompt(
    relayProvider: String,
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        headline = "Stay informed when the app is closed",
        headlineColor = BisqTheme.colors.white,
        message =
            "Get notified about trade updates even when Bisq is not running.\n\n" +
                "Your notification content is encrypted \u2014 $relayProvider only sees " +
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
private fun SimulatedTradeContextScreen(
    tradeId: String,
    relayProvider: String,
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
            relayProvider = relayProvider,
            onEnable = onEnableRelay,
            onDismiss = onDismissPrompt,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun RelayOptInPrompt_Android_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Android prompt (mentions Google):",
                color = BisqTheme.colors.light_grey10,
            )
        }
        SimulatedRelayOptInPrompt(
            relayProvider = "Google",
            onEnable = {},
            onDismiss = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun RelayOptInPrompt_iOS_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "iOS prompt (mentions Apple):",
                color = BisqTheme.colors.light_grey10,
            )
        }
        SimulatedRelayOptInPrompt(
            relayProvider = "Apple",
            onEnable = {},
            onDismiss = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeContext_Android_Preview() {
    BisqTheme.Preview {
        SimulatedTradeContextScreen(
            tradeId = "abc-123-def",
            relayProvider = "Google",
            showPrompt = true,
            onEnableRelay = {},
            onDismissPrompt = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeContext_iOS_Preview() {
    BisqTheme.Preview {
        SimulatedTradeContextScreen(
            tradeId = "abc-123-def",
            relayProvider = "Apple",
            showPrompt = true,
            onEnableRelay = {},
            onDismissPrompt = {},
        )
    }
}
