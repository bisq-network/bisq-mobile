package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqSnackbar(snackbarHostState: SnackbarHostState) {
    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                // containerColor = BisqTheme.colors.primary,
                containerColor = BisqTheme.colors.white.copy(alpha = 0.95f),
                contentColor = BisqTheme.colors.grey2,
                dismissActionContentColor = BisqTheme.colors.grey2,
                modifier = Modifier.padding(bottom = BisqUIConstants.ScreenPadding2X),
            )
        }
    )
}