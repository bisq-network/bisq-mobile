package network.bisq.mobile.presentation.design.welcome_carousel

/**
 * # Analytics Settings Section Design PoC
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Settings section for the analytics opt-in toggle. This is where users can enable
 * or disable crash & usage reporting after the initial carousel prompt, and where
 * they can learn exactly what data is collected and how it's handled.
 *
 * ======================================================================================
 * PLACEMENT IN SETTINGS
 * ======================================================================================
 * The analytics toggle should live in **Settings > General** (or a new "Privacy"
 * sub-section if one is created). Recommended position: after notification settings,
 * before "Reset don't show again" button — grouping all opt-in preferences together.
 *
 * ======================================================================================
 * COMPONENTS
 * ======================================================================================
 *
 * 1. Toggle row: "Crash & Usage Reporting" with ON/OFF switch
 *    - Default: OFF
 *    - Subtitle explaining the value proposition
 *
 * 2. Info card (always visible, below toggle):
 *    - Brief privacy summary:
 *      - Data sent through Tor (no IP leakage)
 *      - No personal information collected
 *      - No trade details, amounts, or addresses
 *      - Auto-deleted after 90 days
 *    - Tappable "Learn more" link → opens external privacy wiki page
 *      (goes through WebLinkConfirmationDialog)
 *
 * ======================================================================================
 * PRESENTER CONTRACT
 * ======================================================================================
 * ```kotlin
 * // In SettingsUiState:
 * val analyticsEnabled: Boolean = false
 *
 * // In SettingsUiAction:
 * data class OnAnalyticsToggle(val enabled: Boolean) : SettingsUiAction
 * ```
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 * mobile.settings.analytics.title          = "Crash & Usage Reporting"
 * mobile.settings.analytics.subtitle       = "Help developers fix bugs and improve the app"
 * mobile.settings.analytics.info.tor       = "Data sent through Tor — your IP is never exposed"
 * mobile.settings.analytics.info.noPii     = "No personal information collected"
 * mobile.settings.analytics.info.noTrade   = "No trade details, amounts, or addresses"
 * mobile.settings.analytics.info.retention = "Data auto-deleted after 90 days"
 * mobile.settings.analytics.learnMore      = "Learn more about data collection"
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ---------------------------------------------------------------------------
// Analytics settings section
// ---------------------------------------------------------------------------

/**
 * Self-contained settings section for analytics opt-in.
 * Drop this into the existing SettingsScreen content column.
 *
 * @param analyticsEnabled Current toggle state
 * @param onToggle Called when user flips the switch
 * @param onLearnMore Called when user taps the "Learn more" link
 */
@Composable
internal fun AnalyticsSettingsSection(
    analyticsEnabled: Boolean,
    onToggle: (Boolean) -> Unit = {},
    onLearnMore: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BisqText.BaseRegular("Crash & Usage Reporting")
                BisqGap.VQuarter()
                BisqText.SmallLightGrey(
                    "Help developers fix bugs and improve the app",
                )
            }
            Switch(
                checked = analyticsEnabled,
                onCheckedChange = onToggle,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = BisqTheme.colors.light_grey10,
                        checkedTrackColor = BisqTheme.colors.primary,
                        uncheckedThumbColor = BisqTheme.colors.mid_grey20,
                        uncheckedTrackColor = BisqTheme.colors.dark_grey50,
                    ),
            )
        }

        BisqGap.V1()

        // Privacy info card
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrivacyInfoRow(
                bullet = "\uD83D\uDD12",
                text = "Data sent through Tor — your IP is never exposed",
            )
            PrivacyInfoRow(
                bullet = "\uD83D\uDEE1\uFE0F",
                text = "No personal information collected",
            )
            PrivacyInfoRow(
                bullet = "\uD83D\uDCB0",
                text = "No trade details, amounts, or addresses",
            )
            PrivacyInfoRow(
                bullet = "\u23F3",
                text = "Data auto-deleted after 90 days",
            )

            BisqGap.VHalf()

            BisqButton(
                text = "Learn more about data collection",
                onClick = onLearnMore,
                type = BisqButtonType.Clear,
                color = BisqTheme.colors.primary,
                padding = PaddingValues(0.dp),
            )
        }
    }
}

@Composable
private fun PrivacyInfoRow(
    bullet: String,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BisqText.SmallRegular(bullet)
        BisqText.SmallLightGrey(text)
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun AnalyticsSettings_Disabled_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            AnalyticsSettingsSection(analyticsEnabled = false)
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun AnalyticsSettings_Enabled_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            AnalyticsSettingsSection(analyticsEnabled = true)
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun AnalyticsSettings_Interactive_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            var enabled by remember { mutableStateOf(false) }
            AnalyticsSettingsSection(
                analyticsEnabled = enabled,
                onToggle = { enabled = it },
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun AnalyticsSettings_InContext_Preview() {
    // Shows how it looks within a settings screen alongside other items
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .padding(BisqUIConstants.ScreenPadding)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Simulated preceding settings item
            BisqText.H5Light("General")
            BisqGap.VHalf()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BisqText.BaseRegular("Notifications")
                BisqText.SmallRegular("Enabled", color = BisqTheme.colors.primary)
            }

            BisqGap.V1()

            // Analytics section
            AnalyticsSettingsSection(analyticsEnabled = false)

            BisqGap.V1()

            // Simulated following settings item
            BisqButton(
                text = "Reset all \"Don't show again\" flags",
                onClick = {},
                type = BisqButtonType.Outline,
                fullWidth = true,
            )
        }
    }
}
