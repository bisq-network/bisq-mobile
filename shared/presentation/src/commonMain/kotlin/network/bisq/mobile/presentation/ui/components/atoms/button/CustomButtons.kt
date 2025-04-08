package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun GreyCloseButton(
    onClick: () -> Unit,
) {
    BisqButton(
        text = "action.close".i18n(),
        type = BisqButtonType.Grey,
        onClick = onClick,
        padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
        fullWidth = true
    )
}