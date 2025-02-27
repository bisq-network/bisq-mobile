package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

// TODO: Confirmation popup
@Composable
fun LinkButton(
    text: String? = "Button",
    onClick: (() -> Unit)? = null,
    fullWidth: Boolean = false,
    modifier: Modifier = Modifier,
) {
    BisqButton(
        text,
        color = BisqTheme.colors.primary,
        type = BisqButtonType.Clear,
        padding = PaddingValues(all = BisqUIConstants.Zero),
        fullWidth = fullWidth,
        onClick = onClick,
        modifier = modifier
    )
}