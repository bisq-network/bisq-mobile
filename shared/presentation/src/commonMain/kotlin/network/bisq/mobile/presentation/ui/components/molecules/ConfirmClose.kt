package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Stable
class ConfirmCloseState internal constructor(
    initial: Boolean = false
) {
    var visible by mutableStateOf(initial)
        internal set

    fun open() { visible = true }
    fun dismiss() { visible = false }

    companion object {
        val Saver: Saver<ConfirmCloseState, Boolean> =
            Saver(save = { it.visible }, restore = { ConfirmCloseState(it) })
    }
}

@Composable
fun rememberConfirmCloseState(): ConfirmCloseState {
    return rememberSaveable(saver = ConfirmCloseState.Saver) {
        ConfirmCloseState(false)
    }
}

/** Top-bar action you can drop into Scaffold.extraActions */
@Composable
fun ConfirmCloseAction(state: ConfirmCloseState) {
    BisqIconButton(
        onClick = { state.open() },
        size = BisqUIConstants.topBarAvatarSize
    ) { CloseIcon() }
}

@Composable
fun ConfirmCloseOverlay(
    state: ConfirmCloseState,
    onConfirmedClose: () -> Unit
) {
    if (!state.visible) return
    ConfirmationDialog(
        onConfirm = {
            state.dismiss()
            onConfirmedClose()
        },
        onDismiss = { state.dismiss() }
    )
}
