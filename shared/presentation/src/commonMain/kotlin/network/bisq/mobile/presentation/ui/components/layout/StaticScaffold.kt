package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqStaticScaffold(
    padding: PaddingValues = PaddingValues(
        top = BisqUIConstants.StaticTopPadding,
        bottom = BisqUIConstants.ScreenPadding,
        start = BisqUIConstants.ScreenPadding,
        end = BisqUIConstants.ScreenPadding
    ),
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = BisqTheme.colors.backgroundColor,
        topBar = topBar ?: {},
        bottomBar = bottomBar ?: {},
        content = {
            BisqStaticLayout(padding = if (topBar != null) it else padding) { content() }
        }
    )
}
