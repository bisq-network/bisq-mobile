package network.bisq.mobile.presentation.ui.components.atoms.layout

import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqVDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 2.dp,
    color: Color = BisqTheme.colors.mid_grey30,
) {
    VerticalDivider(
        thickness = thickness,
        color = color,
        modifier = modifier
    )
}
