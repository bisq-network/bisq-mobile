package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.BisqStepProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.ConfirmCloseAction
import network.bisq.mobile.presentation.ui.components.molecules.ConfirmCloseOverlay
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.rememberConfirmCloseState
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

/**
 * @param stepIndex starts at 1, so last step equals `stepsLength`
 * @param stepsLength How many steps this wizard has
 * @param showUserAvatar whether to show user avatar on TopBar or not
 * @param extraActions extra actions to pass to TopBar. If provided, it takes precedence over the built-in close action.
 * @param closeAction when true, shows a default top-bar close action that opens a confirm-close dialog
 * @param onConfirmedClose invoked after the user confirms the close action; ignored if null
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
    showUserAvatar: Boolean = true,
    extraActions: @Composable (RowScope.() -> Unit)? = null,
    closeAction: Boolean = false,
    onConfirmedClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {

    val confirmClose = rememberConfirmCloseState()
    val extraActionsFinal: (@Composable RowScope.() -> Unit)? =
        extraActions ?: if (closeAction) {
            { ConfirmCloseAction(confirmClose) }
        } else null

    val scaffold: @Composable (
        topBar: @Composable (() -> Unit)?,
        bottomBar: @Composable (() -> Unit)?,
        hAlignment: Alignment.Horizontal,
        vArrangement: Arrangement.Vertical,
        snackbarHostState: SnackbarHostState?,
        jumpToBottom: Boolean,
        shouldBlurBg: Boolean,
        content: @Composable ColumnScope.() -> Unit
    ) -> Unit =
        if (useStaticScaffold) { topBar, bottomBar, hAlignment, verticalArrangement, snackState, _showJumpToBottom, _shouldBlurBg, innerContent ->
            BisqStaticScaffold(
                topBar = topBar,
                bottomBar = bottomBar,
                horizontalAlignment = hAlignment,
                verticalArrangement = verticalArrangement,
                snackbarHostState = snackState,
                isInteractive = isInteractive && !confirmClose.visible,
                shouldBlurBg = _shouldBlurBg || confirmClose.visible,
                content = innerContent
            )
        } else { topBar, bottomBar, hAlignment, verticalArrangement, snackState, _showJumpToBottom, _shouldBlurBg, innerContent ->
            BisqScrollScaffold(
                topBar = topBar,
                bottomBar = bottomBar,
                horizontalAlignment = hAlignment,
                verticalArrangement = verticalArrangement,
                snackbarHostState = snackState,
                isInteractive = isInteractive && !confirmClose.visible,
                showJumpToBottom = _showJumpToBottom,
                shouldBlurBg = _shouldBlurBg || confirmClose.visible,
                content = innerContent
            )
        }

    scaffold(
        {
            Column {
                TopBar(
                    title,
                    showUserAvatar = showUserAvatar,
                    extraActions = extraActionsFinal,
                )
                BisqStepProgressBar(
                    stepIndex = stepIndex,
                    stepsLength = stepsLength,
                )
            }
        },
        {
            if (showNextPrevButtons) {
                BottomAppBar(
                    containerColor = BisqTheme.colors.backgroundColor,
                    contentPadding = PaddingValues(
                        horizontal = BisqUIConstants.ScreenPadding2X,
                        vertical = 0.dp
                    ),
                    windowInsets = WindowInsets(top = 0.dp, bottom = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Lock controls when global interactivity is off or confirm dialog is open.
                        val controlsLocked = !isInteractive || confirmClose.visible
                        var clickLocked by remember { mutableStateOf(false) }
                        // Always reset when we switch wizard steps to avoid sticky locks across steps.
                        if (!androidx.compose.ui.platform.LocalInspectionMode.current) {
                            LaunchedEffect(stepIndex) { clickLocked = false }
                        } else {
                            clickLocked = false
                        }
                        // Auto-unlock throttle when UI is globally interactive (no modal/overlay).
                        if (!androidx.compose.ui.platform.LocalInspectionMode.current) {
                            LaunchedEffect(clickLocked, controlsLocked) {
                                if (clickLocked && !controlsLocked) {
                                    delay(400)
                                    clickLocked = false
                                }
                            }
                        }
                        // Also unlock once the UI becomes interactive again after a modal/overlay.
                        if (!androidx.compose.ui.platform.LocalInspectionMode.current) {
                            LaunchedEffect(controlsLocked) {
                                if (!controlsLocked) clickLocked = false
                            }
                        }

                        BisqButton(
                            text = prevButtonText,
                            type = BisqButtonType.Grey,
                            onClick = {
                                if (prevOnClick != null && !controlsLocked && !clickLocked) {
                                    clickLocked = true
                                    prevOnClick()
                                }
                            },
                            padding = PaddingValues(
                                horizontal = BisqUIConstants.ScreenPaddingHalf,
                                vertical = BisqUIConstants.ScreenPaddingHalf
                            ),
                            disabled = prevOnClick == null || prevDisabled || controlsLocked,
                            modifier = Modifier.weight(1.0F).fillMaxHeight()
                        )
                        BisqGap.H1()
                        BisqButton(
                            text = nextButtonText,
                            onClick = {
                                if (nextOnClick != null && !controlsLocked && !clickLocked) {
                                    clickLocked = true
                                    nextOnClick()
                                }
                            },
                            padding = PaddingValues(
                                horizontal = BisqUIConstants.ScreenPaddingHalf,
                                vertical = BisqUIConstants.ScreenPaddingHalf
                            ),
                            disabled = nextOnClick == null || nextDisabled || controlsLocked,
                            modifier = Modifier.weight(1.0F).fillMaxHeight()
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

    ConfirmCloseOverlay(
        state = confirmClose,
        onConfirmedClose = { onConfirmedClose?.invoke() }
    )
}





