package network.bisq.mobile.presentation.common.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.common.ui.components.molecules.JumpToBottomFloatingButton
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun BisqScrollLayout(
    contentPadding: PaddingValues = PaddingValues(all = BisqUIConstants.ScreenPadding),
    scaffoldPadding: PaddingValues? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    onModifier: ((Modifier) -> Modifier)? = null, // allows to customize modifier settings
    isInteractive: Boolean = true,
    showJumpToBottom: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val jumpToBottomVisible by remember {
        derivedStateOf {
            scrollState.maxValue - scrollState.value > 50
        }
    }

    Box(
        modifier =
            Modifier
                .let {
                    if (scaffoldPadding != null) {
                        it.padding(scaffoldPadding)
                    } else {
                        it
                    }
                }.fillMaxSize()
                .imePadding()
                .background(BisqTheme.colors.backgroundColor),
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            modifier =
                Modifier
                    .fillMaxSize()
                    // .background(color = BisqTheme.colors.backgroundColor)
                    .padding(contentPadding)
                    .verticalScroll(scrollState)
                    .run { onModifier?.invoke(this) ?: this },
        ) {
            content()
        }

        // This covers only the Scaffold content, not the TopBar or BottomBar
        if (!isInteractive) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            coroutineScope {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent() // .consumeAllChanges() // Consumes all touch events
                                    }
                                }
                            }
                        }.clearAndSetSemantics { }, // Disables accessibility interactions
            )
        }

        if (showJumpToBottom) {
            JumpToBottomFloatingButton(
                visible = jumpToBottomVisible,
                onClick = { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = -BisqUIConstants.ScreenPadding),
                jumpOffset = 12,
            )
        }
    }
}
