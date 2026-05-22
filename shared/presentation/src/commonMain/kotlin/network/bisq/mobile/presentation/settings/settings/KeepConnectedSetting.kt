package network.bisq.mobile.presentation.settings.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSwitch
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * The "keep connected in background" sub-setting under push notifications.
 *
 * Visible only when relayed push is enabled AND the platform supports it
 * (Android Connect). Renders as an indented switch with a trade-off subtitle.
 * The 16dp leading indent communicates the parent-toggle dependency without a
 * heavier card / divider.
 *
 * See [SettingsPresenter.shouldShowKeepConnectedToggle] for the platform gate
 * and [SettingsUiState.pushNotificationsEnabled] for the parent-toggle gate;
 * both must be true for this to render.
 */
@ExcludeFromCoverage
@Composable
fun KeepConnectedSetting(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(start = BisqUIConstants.ScreenPadding)
                .fillMaxWidth(),
    ) {
        BisqSwitch(
            label = "mobile.pushNotifications.settings.keepConnected.toggleLabel".i18n(),
            checked = enabled,
            onSwitch = onToggle,
        )

        BisqGap.VQuarter()

        BisqText.SmallLight(
            text = "mobile.pushNotifications.settings.keepConnected.subtitle".i18n(),
            color = BisqTheme.colors.mid_grey20,
        )
    }
}
