package network.bisq.mobile.presentation.ui.components.molecules.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun SettingsButton(label: String, onClick: () -> Unit) {
    BisqButton(
        label,
        onClick = onClick,
        fullWidth = true,
        backgroundColor = BisqTheme.colors.dark4,
        cornerRadius = BisqUIConstants.Zero,
        rightIcon = { ArrowRightIcon() },
        textAlign = TextAlign.Start,
        padding = PaddingValues(all = BisqUIConstants.ScreenPadding)
    )
}