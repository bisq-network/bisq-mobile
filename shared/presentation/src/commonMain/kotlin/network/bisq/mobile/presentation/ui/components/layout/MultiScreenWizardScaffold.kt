package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

/**
 * @param stepIndex starts at 1, so last step equals `stepsLength`
 * @param stepsLength How many steps this wizard has
 */
@Composable
fun MultiScreenWizardScaffold(
    title: String,
    stepIndex: Int,
    stepsLength: Int,
    prevButtonText: String = "action.back".i18n(),
    nextButtonText: String = "action.next".i18n(),
    prevOnClick: (() -> Unit)? = null,
    nextOnClick: (() -> Unit)? = null,
    prevDisabled: Boolean = false,
    nextDisabled: Boolean = false,
    showNextPrevButtons: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    useStaticScaffold: Boolean = false,
    snackbarHostState: SnackbarHostState? = null,
    isInteractive: Boolean = true,
    showJumpToBottom: Boolean = false,
    shouldBlurBg: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {

    val scaffold: @Composable (
        padding: PaddingValues,
        topBar: @Composable (() -> Unit)?,
        bottomBar: @Composable (() -> Unit)?,
        hAlignment: Alignment.Horizontal,
        vArrangement: Arrangement.Vertical,
        snackbarHostState: SnackbarHostState?,
        jumpToBottom: Boolean,
        shouldBlurBg: Boolean,
        content: @Composable ColumnScope.() -> Unit
    ) -> Unit =
        if (useStaticScaffold) { padding, topBar, bottomBar, hAlignment, verticalArrangement, snackState, _showJumpToBottom, _shouldBlurBg, innerContent ->
            BisqStaticScaffold(
                padding = padding,
                topBar = topBar,
                bottomBar = bottomBar,
                horizontalAlignment = hAlignment,
                verticalArrangement = verticalArrangement,
                snackbarHostState = snackState,
                isInteractive = isInteractive,
                shouldBlurBg = _shouldBlurBg,
                content = innerContent
            )
        } else { padding, topBar, bottomBar, hAlignment, verticalArrangement, snackState, _showJumpToBottom, _shouldBlurBg, innerContent ->
            BisqScrollScaffold(
                padding = padding,
                topBar = topBar,
                bottomBar = bottomBar,
                horizontalAlignment = hAlignment,
                verticalArrangement = verticalArrangement,
                snackbarHostState = snackState,
                isInteractive = isInteractive,
                showJumpToBottom = _showJumpToBottom,
                shouldBlurBg = _shouldBlurBg,
                content = innerContent
            )
        }

    scaffold(
        PaddingValues(all = 0.dp),
        {
            Column {
                TopBar(title, isFlowScreen = true, stepText = "$stepIndex/$stepsLength")
                BisqProgressBar(
                    stepIndex.toFloat() / stepsLength.toFloat(),
                    modifier = Modifier.fillMaxWidth().padding(top = BisqUIConstants.ScreenPaddingHalf)
                )
            }
        },
        {
            if (showNextPrevButtons) {
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
                            type = BisqButtonType.Grey,
                            onClick = {
                                if (prevOnClick != null) {
                                    prevOnClick()
                                }
                                      },
                            padding = PaddingValues(
                                horizontal = BisqUIConstants.ScreenPaddingHalf,
                                vertical = BisqUIConstants.ScreenPaddingHalf
                            ),
                            disabled = prevOnClick == null || prevDisabled,
                            modifier = Modifier.weight(1.0F)
                        )
                        BisqGap.H1()
                        BisqButton(
                            text = nextButtonText,
                            onClick = {
                                if (nextOnClick != null) {
                                    nextOnClick()
                                }
                                      },
                            padding = PaddingValues(
                                horizontal = BisqUIConstants.ScreenPaddingHalf,
                                vertical = BisqUIConstants.ScreenPaddingHalf
                            ),
                            disabled = nextOnClick == null || nextDisabled,
                            modifier = Modifier.weight(1.0F)
                        )
                    }
                }
            }
        },
        horizontalAlignment,
        Arrangement.Top,
        snackbarHostState,
        showJumpToBottom,
        shouldBlurBg,
    ) {

        // TODO: Should pass these values to the column deep inside BisqScrollLayout
        // as BissScrollScaffold's params, rather than creating a column here?

        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.Top,
        ) {
            content()
        }

    }
}





