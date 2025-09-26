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
fun BisqScrollScaffold(
    padding: PaddingValues = PaddingValues(
        top = BisqUIConstants.ScrollTopPadding,
        bottom = BisqUIConstants.ScreenPadding,
        start = BisqUIConstants.ScreenPadding,
        end = BisqUIConstants.ScreenPadding
    ),
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    fab: @Composable (() -> Unit)? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    isInteractive: Boolean = true,
    showJumpToBottom: Boolean = false,
    shouldBlurBg: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = Modifier
            .then(
                if (shouldBlurBg) Modifier.blur(BisqUIConstants.ScreenPaddingHalf)
                else Modifier
            ),
        containerColor = BisqTheme.colors.backgroundColor,
        topBar = topBar ?: {},
        bottomBar = bottomBar ?: {},
        snackbarHost = {
            if (snackbarHostState != null) {
                BisqSnackbar(snackbarHostState = snackbarHostState)
            }
        },
        floatingActionButton = fab ?: {},
        content = { scaffoldPadding ->
            BisqScrollLayout(
                scaffoldPadding = scaffoldPadding,
                contentPadding = padding,
                verticalArrangement = verticalArrangement,
                isInteractive = isInteractive,
                showJumpToBottom = showJumpToBottom,
                horizontalAlignment = horizontalAlignment
            ) {
                content()
            }
        }
    )
}
