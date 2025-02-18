package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.components.atoms.icons.AddIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqFABButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(BisqUIConstants.ScreenPadding4X),
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        modifier = modifier,
        onClick = onClick,
        containerColor = BisqTheme.colors.primary,
        contentColor = BisqTheme.colors.white,
        shape = CircleShape
    ) {
        content()
    }
}

@Composable
fun BisqFABAddButton(
    onClick: () -> Unit,
) {
    BisqFABButton(
        onClick = onClick,
    ) {
        AddIcon()
    }
}