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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSwitch
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Design POC: Notifications section in Settings screen with relay opt-in toggle.
 *
 * Shown on both platforms with platform-appropriate copy:
 * - Android: FCM relay through Google
 * - iOS: APNs relay through Apple
 *
 * Default is OFF on both platforms (privacy-first). A tappable "Learn more" link
 * opens a bottom sheet explaining the privacy trade-off.
 *
 * On iOS, an additional note clarifies that relayed notifications are the only way
 * to receive alerts when the app is closed (no P2P fallback exists on iOS).
 *
 * Key constraint: existing notification flows continue as-is. Relay registration
 * is only triggered when the user explicitly opts in via this toggle.
 */

private enum class SimulatedPlatform(
    val relayProvider: String,
    val hasLocalFallback: Boolean,
) {
    ANDROID("Google", true),
    IOS("Apple", false),
}

@Composable
private fun NotificationsSettingsSection(
    platform: SimulatedPlatform,
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

        if (!platform.hasLocalFallback) {
            BisqGap.VQuarter()
            BisqText.SmallLight(
                text =
                    "On iOS, this is the only way to receive notifications " +
                        "when the app is not running.",
                color = BisqTheme.colors.mid_grey20,
            )
        }

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
private fun LearnMoreBottomSheetContent(platform: SimulatedPlatform) {
    val provider = platform.relayProvider

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H5Light("How relayed push notifications work")

        BisqGap.V1()

        BisqText.SmallLight(
            text = "What is shared with $provider",
            color = BisqTheme.colors.light_grey10,
        )
        BisqGap.VQuarter()
        BulletItem("Your device\u2019s notification token")
        BulletItem("Encrypted notification payloads (content unreadable)")
        BulletItem("Delivery timestamps")

        BisqGap.V1()

        BisqText.SmallLight(
            text = "What $provider cannot see",
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
                        "Bisq node and this device. $provider only delivers an " +
                        "opaque encrypted payload.",
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
    platform: SimulatedPlatform,
    relayEnabled: Boolean,
    onRelayToggle: (Boolean) -> Unit,
    showLearnMore: Boolean,
    onLearnMoreClick: () -> Unit,
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
            platform = platform,
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
            LearnMoreBottomSheetContent(platform = platform)
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun Android_Disabled_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            platform = SimulatedPlatform.ANDROID,
            relayEnabled = false,
            onRelayToggle = {},
            showLearnMore = false,
            onLearnMoreClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun Android_Enabled_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            platform = SimulatedPlatform.ANDROID,
            relayEnabled = true,
            onRelayToggle = {},
            showLearnMore = false,
            onLearnMoreClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun Android_LearnMore_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            platform = SimulatedPlatform.ANDROID,
            relayEnabled = false,
            onRelayToggle = {},
            showLearnMore = true,
            onLearnMoreClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun IosDisabled_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            platform = SimulatedPlatform.IOS,
            relayEnabled = false,
            onRelayToggle = {},
            showLearnMore = false,
            onLearnMoreClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun IosEnabled_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            platform = SimulatedPlatform.IOS,
            relayEnabled = true,
            onRelayToggle = {},
            showLearnMore = false,
            onLearnMoreClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun IosLearnMore_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            platform = SimulatedPlatform.IOS,
            relayEnabled = false,
            onRelayToggle = {},
            showLearnMore = true,
            onLearnMoreClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun Interactive_Preview() {
    var relayEnabled by remember { mutableStateOf(false) }
    var showLearnMore by remember { mutableStateOf(false) }
    BisqTheme.Preview {
        SettingsScreenWithNotifications(
            platform = SimulatedPlatform.ANDROID,
            relayEnabled = relayEnabled,
            onRelayToggle = { relayEnabled = it },
            showLearnMore = showLearnMore,
            onLearnMoreClick = { showLearnMore = !showLearnMore },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun LearnMoreSheet_Android_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.dark_grey50),
        ) {
            LearnMoreBottomSheetContent(platform = SimulatedPlatform.ANDROID)
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun LearnMoreSheet_iOS_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.dark_grey50),
        ) {
            LearnMoreBottomSheetContent(platform = SimulatedPlatform.IOS)
        }
    }
}
