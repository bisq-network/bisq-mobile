package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.coroutineScope
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqStaticLayout(
    contentPadding: PaddingValues = PaddingValues(all = BisqUIConstants.ScreenPadding),
    scaffoldPadding: PaddingValues? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.SpaceBetween,
    isInteractive: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .let {
                if (scaffoldPadding != null) {
                    it.padding(scaffoldPadding)
                } else {
                    it
                }
            }
            .imePadding()
            .background(BisqTheme.colors.backgroundColor)
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            modifier = Modifier
                .fillMaxSize()
                .background(color = BisqTheme.colors.backgroundColor)
                .padding(contentPadding)
        ) {
            content()
        }

        // This covers only the Scaffold content, not the TopBar or BottomBar
        if (!isInteractive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        coroutineScope {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                    }
                    .clearAndSetSemantics { } // Disables accessibility interactions
            )
        }
    }
}
