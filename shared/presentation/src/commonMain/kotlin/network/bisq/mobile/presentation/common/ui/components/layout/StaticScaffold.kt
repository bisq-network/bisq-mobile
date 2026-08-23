package network.bisq.mobile.presentation.common.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

/**
 * Non-scrolling flavor of [BisqScaffold], which owns the shared scaffold behavior
 * (IME insets, snackbar, blur). This wrapper only adds the [BisqStaticLayout] content slot;
 * [BisqScrollScaffold] is its scrolling sibling.
 */
@Composable
fun BisqStaticScaffold(
    padding: PaddingValues =
        PaddingValues(
            top = BisqUIConstants.ScreenPadding,
            bottom = BisqUIConstants.ScreenPadding,
            start = BisqUIConstants.ScreenPadding,
            end = BisqUIConstants.ScreenPadding,
        ),
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    floatingButton: @Composable (() -> Unit)? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    shouldBlurBg: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    BisqScaffold(
        topBar = topBar ?: {},
        bottomBar = bottomBar ?: {},
        snackbarHostState = snackbarHostState,
        floatingActionButton = floatingButton ?: {},
        shouldBlurBg = shouldBlurBg,
    ) { scaffoldPadding ->
        BisqStaticLayout(
            contentPadding = padding,
            scaffoldPadding = scaffoldPadding,
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
        ) {
            content()
        }
    }
}
