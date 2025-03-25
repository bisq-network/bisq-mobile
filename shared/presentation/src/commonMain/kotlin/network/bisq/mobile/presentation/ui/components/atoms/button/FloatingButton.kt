package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.presentation.ui.components.atoms.icons.AddIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun FloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(BisqUIConstants.ScreenPadding4X),
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        modifier = modifier,
        onClick = { if (enabled) onClick() },
        containerColor = if (enabled) BisqTheme.colors.primary else Color.Gray.copy(alpha = 0.5f),
        contentColor = if (enabled) BisqTheme.colors.white else Color.LightGray,
        shape = CircleShape,
    ) {
        content()
    }
}

@Composable
fun BisqFABAddButton(
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    FloatingButton(
        onClick = onClick,
        enabled = enabled
    ) {
        AddIcon()
    }
}