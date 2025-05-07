package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
        top = BisqUIConstants.StaticTopPadding,
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
        content = {
            BisqStaticLayout(
                padding = if (topBar != null) it else padding,
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = verticalArrangement,
                isInteractive = isInteractive
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
        }
    )
}
