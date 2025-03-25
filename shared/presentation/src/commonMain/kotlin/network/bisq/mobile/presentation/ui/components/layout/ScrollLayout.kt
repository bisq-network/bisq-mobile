package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.ui.components.molecules.JumpToBottomFloatingButton
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqScrollLayout(
    padding: PaddingValues = PaddingValues(all = BisqUIConstants.ScreenPadding),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    onModifier: ((Modifier) -> Modifier)? = null, // allows to customize modifier settings
    isInteractive: Boolean = true,
    showJumpToBottom: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val jumpThreshold = with(LocalDensity.current) {
        JumpToBottomThreshold.toPx()
    }

    val jumpToBottomButtonEnabled by remember {
        derivedStateOf {
            scrollState.maxValue - scrollState.value > 50
        }
    }

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
                .verticalScroll(scrollState)
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

        if (showJumpToBottom) {
            JumpToBottomFloatingButton(
                enabled = jumpToBottomButtonEnabled,
                onClicked = { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private val JumpToBottomThreshold = 16.dp
