package network.bisq.mobile.presentation.design.push_notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSwitch
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Design POC: Notifications section in Settings screen with FCM relay opt-in toggle.
 *
 * Adds a "Notifications" section to Settings containing a toggle for relayed push
 * notifications (FCM on Android). Default is OFF (privacy-first). A tappable "Learn more"
 * link opens a bottom sheet explaining the privacy trade-off.
 *
 * Key constraint: existing push notification flow continues to work as-is. FCM
 * registration is only triggered when the user explicitly opts in via this toggle.
 *
 * Android-only: this section is not shown on iOS (APNs handled at pairing time).
 */

@Composable
private fun NotificationsSettingsSection(
    relayEnabled: Boolean,
    onRelayToggle: (Boolean) -> Unit,
    onLearnMoreClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BisqHDivider()

        BisqGap.V1()

        BisqText.H4Light("Notifications")

        BisqGap.V1()

        BisqSwitch(
            label = "Relayed push notifications",
            checked = relayEnabled,
            onSwitch = onRelayToggle,
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
            modifier = Modifier.clickable { onLearnMoreClick() },
        )

        if (!relayEnabled) {
            BisqGap.VHalf()
            BisqText.SmallLight(
                text = "You may miss trade messages while the app is closed.",
                color = BisqTheme.colors.warning,
            )
        }
    }
}

@Composable
private fun LearnMoreBottomSheetContent() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H5Light("How relayed push notifications work")

        BisqGap.V1()

        BisqText.SmallLight(
            text = "What is shared with Google",
            color = BisqTheme.colors.light_grey10,
        )
        BisqGap.VQuarter()
        BulletItem("Your device\u2019s notification token")
        BulletItem("Encrypted notification payloads (content unreadable)")
        BulletItem("Delivery timestamps")

        BisqGap.V1()

        BisqText.SmallLight(
            text = "What Google cannot see",
            color = BisqTheme.colors.light_grey10,
        )
        BisqGap.VQuarter()
        BulletItem("Your trade details or amounts")
        BulletItem("Your counterparty\u2019s identity")
        BulletItem("Your payment information")

        BisqGap.V1()

        Row(verticalAlignment = Alignment.Top) {
            InfoGreenIcon(modifier = Modifier.size(16.dp))
            BisqGap.HHalf()
            BisqText.SmallLight(
                text =
                    "Notification content is end-to-end encrypted between your " +
                        "Bisq node and this device. This is the same security model " +
                        "used for all Bisq communications.",
                color = BisqTheme.colors.mid_grey20,
            )
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    BisqText.SmallLight(
        text = "\u2022  $text",
        color = BisqTheme.colors.mid_grey20,
    )
}

@Composable
private fun SettingsScreenWithNotifications(
    relayEnabled: Boolean,
    onRelayToggle: (Boolean) -> Unit,
    showLearnMore: Boolean,
    onLearnMoreClick: () -> Unit,
    onDismissLearnMore: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor)
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H4Light("Display")
        BisqGap.V1()
        BisqSwitch(
            label = "Use animations",
            checked = true,
            onSwitch = {},
        )

        BisqGap.V1()

        NotificationsSettingsSection(
            relayEnabled = relayEnabled,
            onRelayToggle = onRelayToggle,
            onLearnMoreClick = onLearnMoreClick,
        )

        if (showLearnMore) {
            BisqGap.V1()
            BisqHDivider()
            BisqGap.VHalf()
            BisqText.XSmallMedium(
                text = "[ Bottom sheet would appear here \u2193 ]",
                color = BisqTheme.colors.mid_grey20,
            )
            BisqGap.VHalf()
            LearnMoreBottomSheetContent()
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NotificationSettings_Disabled_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            relayEnabled = false,
            onRelayToggle = {},
            showLearnMore = false,
            onLearnMoreClick = {},
            onDismissLearnMore = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NotificationSettings_Enabled_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            relayEnabled = true,
            onRelayToggle = {},
            showLearnMore = false,
            onLearnMoreClick = {},
            onDismissLearnMore = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NotificationSettings_LearnMore_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            relayEnabled = false,
            onRelayToggle = {},
            showLearnMore = true,
            onLearnMoreClick = {},
            onDismissLearnMore = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NotificationSettings_Interactive_Preview() {
    var relayEnabled by remember { mutableStateOf(false) }
    var showLearnMore by remember { mutableStateOf(false) }
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            relayEnabled = relayEnabled,
            onRelayToggle = { relayEnabled = it },
            showLearnMore = showLearnMore,
            onLearnMoreClick = { showLearnMore = !showLearnMore },
            onDismissLearnMore = { showLearnMore = false },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun LearnMoreSheet_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.dark_grey50),
        ) {
            LearnMoreBottomSheetContent()
        }
    }
}
