package network.bisq.mobile.presentation.common.ui.components.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.components.organisms.BisqSnackbar
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun BisqScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit) = {},
    bottomBar: @Composable (() -> Unit) = {},
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: @Composable (() -> Unit) = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding(),
        topBar = topBar,
        bottomBar = bottomBar,
        containerColor = BisqTheme.colors.backgroundColor,
        snackbarHost = {
            if (snackbarHostState != null) {
                BisqSnackbar(snackbarHostState = snackbarHostState)
            }
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}
