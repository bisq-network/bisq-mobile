package network.bisq.mobile.presentation.ui.components.atoms.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqHDivider(
    verticalPadding: Dp = BisqUIConstants.ScreenPadding2X,
    modifier: Modifier = Modifier.padding(vertical = verticalPadding)
) {
    HorizontalDivider(
        thickness = 1.dp,
        modifier = modifier,
        color = BisqTheme.colors.mid_grey10
    )
}