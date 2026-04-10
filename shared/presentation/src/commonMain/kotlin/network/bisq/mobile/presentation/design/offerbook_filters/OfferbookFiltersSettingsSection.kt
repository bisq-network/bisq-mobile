package network.bisq.mobile.presentation.design.offerbook_filters

import androidx.compose.foundation.background
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
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSwitch
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Design POC: Offerbook section in Settings screen.
 *
 * Adds a new "Offerbook" section after the Display section, containing a single toggle
 * to control whether offer filter preferences (payment/settlement method selections)
 * are persisted across app sessions.
 *
 * ## Behavior
 * - Default: ON (most users expect preferences to survive app restart)
 * - Toggle OFF: immediately clears all stored filter preferences; current session unaffected
 * - Help text clarifies exactly what is saved (method selections per market)
 *
 * ## i18n keys needed
 * - mobile.settings.offerbook.headline = "Offerbook"
 * - mobile.settings.offerbook.persistFilters = "Remember filter preferences"
 * - mobile.settings.offerbook.persistFilters.help = "Saves your payment and settlement
 *   method selections for each market across sessions."
 */

private data class SimulatedOfferbookSettings(
    val persistFilterPreferences: Boolean = true,
)

/**
 * The Offerbook settings section as it would appear within the full Settings screen.
 * Shows a toggle for persisting filter preferences with explanatory help text.
 */
@Composable
private fun OfferbookSettingsSection(
    settings: SimulatedOfferbookSettings,
    onPersistFiltersChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BisqHDivider()

        BisqGap.V1()

        BisqText.H4Light("Offerbook")

        BisqGap.V1()

        BisqSwitch(
            label = "Remember filter preferences",
            checked = settings.persistFilterPreferences,
            onSwitch = onPersistFiltersChange,
        )

        BisqGap.VQuarter()

        BisqText.SmallLight(
            text = "Saves your payment and settlement method selections for each market across sessions.",
            color = BisqTheme.colors.mid_grey20,
        )
    }
}

/**
 * Shows the Offerbook section in context — preceded by a simulated Display section
 * so reviewers can see how the new section fits within the existing settings layout.
 */
@Composable
private fun SettingsScreenWithOfferbookSection(
    settings: SimulatedOfferbookSettings,
    onPersistFiltersChange: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding,
                ),
    ) {
        // Simulated Display section (existing)
        BisqText.H4Light("Display")
        BisqGap.V1()
        BisqSwitch(
            label = "Use animations",
            checked = true,
            onSwitch = {},
        )

        BisqGap.V1()

        // New Offerbook section
        OfferbookSettingsSection(
            settings = settings,
            onPersistFiltersChange = onPersistFiltersChange,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferbookSettings_Enabled_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithOfferbookSection(
            settings = SimulatedOfferbookSettings(persistFilterPreferences = true),
            onPersistFiltersChange = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferbookSettings_Disabled_Preview() {
    BisqTheme.Preview {
        SettingsScreenWithOfferbookSection(
            settings = SimulatedOfferbookSettings(persistFilterPreferences = false),
            onPersistFiltersChange = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OfferbookSettings_Interactive_Preview() {
    var settings by remember { mutableStateOf(SimulatedOfferbookSettings()) }
    BisqTheme.Preview {
        SettingsScreenWithOfferbookSection(
            settings = settings,
            onPersistFiltersChange = { settings = settings.copy(persistFilterPreferences = it) },
        )
    }
}
