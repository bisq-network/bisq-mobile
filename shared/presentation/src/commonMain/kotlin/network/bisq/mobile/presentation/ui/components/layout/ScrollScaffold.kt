package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = BisqTheme.colors.backgroundColor,
        topBar = topBar ?: {},
        bottomBar = bottomBar ?: {},
        content = {
            BisqScrollLayout(
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
                        horizontalAlignment = horizontalAlignment
                    ) {
                        content()
                    }
                else
                    content()
            }
        },
    )
}
