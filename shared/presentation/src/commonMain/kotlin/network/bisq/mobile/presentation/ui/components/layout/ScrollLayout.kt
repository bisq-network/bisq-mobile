package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqScrollLayout(
    padding: PaddingValues = PaddingValues(all = BisqUIConstants.ScreenPadding),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    onModifier: ((Modifier) -> Modifier)? = null, // allows to customize modifier settings
    isInteractive: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BisqTheme.colors.backgroundColor)
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            modifier = Modifier
                .fillMaxSize()
                // .background(color = BisqTheme.colors.backgroundColor)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .run { onModifier?.invoke(this) ?: this }
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
                                    awaitPointerEvent()// .consumeAllChanges() // Consumes all touch events
                                }
                            }
                        }
                    }
                    .clearAndSetSemantics { } // Disables accessibility interactions
            )
        }
    }
}
