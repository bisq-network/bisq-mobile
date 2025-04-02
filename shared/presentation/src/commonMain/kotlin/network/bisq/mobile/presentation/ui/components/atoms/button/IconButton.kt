package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    content: @Composable () -> Unit
) {
    IconButton(
        modifier = Modifier
            .size(BisqUIConstants.ScreenPadding2X)
            .alpha(if (disabled) 0.5f else 1.0f),
        onClick = onClick,
        colors = IconButtonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = BisqTheme.colors.white,
            disabledContentColor = BisqTheme.colors.mid_grey20,
        ),
        enabled = !disabled
    ) {
        content()
    }
}