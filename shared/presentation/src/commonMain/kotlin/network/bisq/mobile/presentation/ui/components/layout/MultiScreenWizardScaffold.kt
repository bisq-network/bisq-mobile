package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun MultiScreenWizardScaffold(
    title: String,
    stepIndex: Int,
    stepsLength: Int,
    prevButtonText: String = LocalStrings.current.common.buttons_back,
    nextButtonText: String = LocalStrings.current.common.buttons_next,
    prevOnClick: (() -> Unit)? = null,
    nextOnClick: (() -> Unit)? = null,
    prevDisabled: Boolean = false,
    nextDisabled: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    useStaticScaffold: Boolean = false,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit
) {

    val scaffold: @Composable (
        padding: PaddingValues,
        topBar: @Composable (() -> Unit)?,
        bottomBar: @Composable (() -> Unit)?,
        hAlignment: Alignment.Horizontal,
        vArrangement: Arrangement.Vertical,
        snackbarHostState: SnackbarHostState?,
        content: @Composable ColumnScope.() -> Unit
    ) -> Unit =
        if (useStaticScaffold) { padding, topBar, bottomBar, hAlignment, verticalArrangement, snackState, innerContent ->
            BisqStaticScaffold(
                padding = padding,
                topBar = topBar,
                bottomBar = bottomBar,
                horizontalAlignment = hAlignment,
                verticalArrangement = verticalArrangement,
                snackbarHostState = snackState,
                content = innerContent
            )
        } else { padding, topBar, bottomBar, hAlignment, verticalArrangement, snackState, innerContent ->
            BisqScrollScaffold(
                padding = padding,
                topBar = topBar,
                bottomBar = bottomBar,
                horizontalAlignment = hAlignment,
                verticalArrangement = verticalArrangement,
                snackbarHostState = snackState,
                content = innerContent
            )
        }

    scaffold(
        PaddingValues(all = 0.dp),
        {
            TopBar(title, isFlowScreen = true, stepText = "$stepIndex/$stepsLength")
        },
        {
            // TODO: This takes up too much height
            BottomAppBar(
                containerColor = BisqTheme.colors.backgroundColor,
                contentPadding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding2X, vertical = 0.dp),
                windowInsets = WindowInsets(top = 0.dp, bottom = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BisqButton(
                        text = prevButtonText,
                        backgroundColor = BisqTheme.colors.dark5,
                        onClick = {
                            if (prevOnClick != null) {
                                prevOnClick()
                            }
                        },
                        padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding3X, vertical = BisqUIConstants.ScreenPaddingHalf),
                        disabled = prevOnClick == null || prevDisabled
                    )
                    BisqButton(
                        text = nextButtonText,
                        onClick = {
                            if (nextOnClick != null) {
                                nextOnClick()
                            }
                        },
                        padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding3X, vertical = BisqUIConstants.ScreenPaddingHalf),
                        disabled = nextOnClick == null || nextDisabled
                    )
                }
            }

        },
        horizontalAlignment,
        Arrangement.Top,
        snackbarHostState,
    ) {

        BisqProgressBar(
            stepIndex.toFloat() / stepsLength.toFloat(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        // TODO: Should pass these values to the column deep inside BisqScrollLayout
        // as BissScrollScaffold's params, rather than creating a column here?

        Column(
            modifier = Modifier.fillMaxHeight().padding(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPadding
            ),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.Top,
        ) {
            content()
        }

    }
}





