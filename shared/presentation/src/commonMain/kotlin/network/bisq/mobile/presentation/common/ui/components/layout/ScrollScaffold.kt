package network.bisq.mobile.presentation.common.ui.components.layout

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

/**
 * Scrolling flavor of [BisqScaffold], which owns the shared scaffold behavior
 * (IME insets, snackbar, blur). This wrapper only adds the [BisqScrollLayout] content slot;
 * [BisqStaticScaffold] is its non-scrolling sibling.
 */
@Composable
fun BisqScrollScaffold(
    modifier: Modifier = Modifier,
    padding: PaddingValues =
        PaddingValues(
            top = BisqUIConstants.ScrollTopPadding,
            bottom = BisqUIConstants.ScreenPadding,
            start = BisqUIConstants.ScreenPadding,
            end = BisqUIConstants.ScreenPadding,
        ),
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    fab: @Composable (() -> Unit)? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    showJumpToBottom: Boolean = false,
    shouldBlurBg: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    BisqScaffold(
        modifier = modifier,
        topBar = topBar ?: {},
        bottomBar = bottomBar ?: {},
        snackbarHostState = snackbarHostState,
        floatingActionButton = fab ?: {},
        shouldBlurBg = shouldBlurBg,
    ) { scaffoldPadding ->
        BisqScrollLayout(
            scaffoldPadding = scaffoldPadding,
            contentPadding = padding,
            verticalArrangement = verticalArrangement,
            showJumpToBottom = showJumpToBottom,
            horizontalAlignment = horizontalAlignment,
            scrollState = scrollState,
        ) {
            content()
        }
    }
}
