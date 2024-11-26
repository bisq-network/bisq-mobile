package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqStaticScaffold(
    innerPadding: PaddingValues = PaddingValues(top = 96.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = BisqTheme.colors.backgroundColor,
        topBar = topBar,
        bottomBar = bottomBar,
        content = {
            BisqStaticLayout(innerPadding = innerPadding) { content() }
        }
    )
}
