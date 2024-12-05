package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme



@Composable
fun BisqVDivider() {
    VerticalDivider(
        thickness = 2.dp,
        color = BisqTheme.colors.grey3,
        modifier = Modifier.height(108.dp)
        // modifier = Modifier.fillMaxHeight()
    )
}
