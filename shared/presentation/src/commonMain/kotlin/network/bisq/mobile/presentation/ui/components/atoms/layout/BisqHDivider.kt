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
    verticalPadding: Dp = BisqUIConstants.ScreenPadding2X
) {
    HorizontalDivider(
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = verticalPadding),
        color = BisqTheme.colors.mid_grey10
    )
}