package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import network.bisq.mobile.presentation.ui.components.organisms.BisqSnackbar
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

// FinalTODO: Merge StaticScaffold and ScrollScaffold
@Composable
fun BisqStaticScaffold(
    padding: PaddingValues = PaddingValues(
        top = BisqUIConstants.ScreenPadding,
        bottom = BisqUIConstants.ScreenPadding,
        start = BisqUIConstants.ScreenPadding,
        end = BisqUIConstants.ScreenPadding
    ),
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    floatingButton: @Composable (() -> Unit)? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    isInteractive: Boolean = true,
    shouldBlurBg: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = Modifier.blur(if (shouldBlurBg) BisqUIConstants.ScreenPaddingHalf else BisqUIConstants.Zero),
        containerColor = BisqTheme.colors.backgroundColor,
        topBar = topBar ?: {},
        bottomBar = bottomBar ?: {},
        snackbarHost = {
            if (snackbarHostState != null) {
                BisqSnackbar(snackbarHostState = snackbarHostState)
            }
        },
        floatingActionButton = floatingButton ?: {},
        content = { scaffoldPadding ->
            BisqStaticLayout(
                contentPadding = padding,
                scaffoldPadding = scaffoldPadding,
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = verticalArrangement,
                isInteractive = isInteractive
            ) {
                content()
            }
        }
    )
}
