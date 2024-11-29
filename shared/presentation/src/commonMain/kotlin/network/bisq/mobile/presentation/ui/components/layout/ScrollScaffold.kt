package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

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
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = BisqTheme.colors.backgroundColor,
        topBar = topBar ?: {},
        bottomBar = bottomBar ?: {},
        content = {
            BisqScrollLayout(padding = if (topBar != null) it else padding) { content() }
        },
    )
}
