package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = BisqTheme.colors.backgroundColor,
        topBar = topBar ?: {},
        bottomBar = bottomBar ?: {},
        content = { it ->
            BisqStaticLayout(
                padding = if (topBar != null) it else padding,
                verticalArrangement = verticalArrangement
            ) {
                // Padding logic:
                // when topBar is set, Scaffold.content.it provides the padding
                // to offset topBar height, which is passed to BisqStaticLayout
                // But then the content()'s get attached to the screen edges.
                // So in that case, we add another column to provde ScreenPadding on all sides.
                if (topBar != null)
                    Column(
                        modifier = Modifier.padding(all = BisqUIConstants.ScreenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        content()
                    }
                else
                    content()
            }
        }
    )
}
