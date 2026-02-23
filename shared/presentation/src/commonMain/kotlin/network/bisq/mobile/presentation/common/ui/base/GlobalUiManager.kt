package network.bisq.mobile.presentation.common.ui.base

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType

/**
 * Sealed class representing all snackbar actions for the unified SharedFlow
 */
sealed class SnackbarAction {
    data class Show(
        val message: String,
        val type: SnackbarType = SnackbarType.SUCCESS,
        val duration: SnackbarDuration = SnackbarDuration.Short,
    ) : SnackbarAction()

    data object Dismiss : SnackbarAction()
}

/**
 * Global UI state manager for app-wide UI elements like loading dialogs and snackbars.
 * This is injected into presenters to manage global UI state without coupling to BasePresenter.
 * Uses its own coroutine scope for UI operations.
 */
class GlobalUiManager(
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _showLoadingDialog = MutableStateFlow(false)
    val showLoadingDialog: StateFlow<Boolean> = _showLoadingDialog.asStateFlow()

    private var loadingJob: Job? = null

    // Snackbar actions as SharedFlow for one-time events (buffer holds 1, drops oldest on overflow)
    private val _snackbarActions =
        MutableSharedFlow<SnackbarAction>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val snackbarActions: SharedFlow<SnackbarAction> = _snackbarActions.asSharedFlow()

    /**
     * Schedule showing a loading dialog after a grace delay.
     * If the operation completes before the delay expires, the dialog never appears (avoiding flicker).
     * Call hideLoading() when the operation completes to cancel the scheduled show and hide the dialog.
     */
    fun scheduleShowLoading() {
        loadingJob?.cancel()
        loadingJob =
            scope.launch {
                delay(LOADING_DIALOG_GRACE_MS)
                _showLoadingDialog.value = true
            }
    }

    /**
     * Hide the loading dialog and cancel any scheduled show.
     */
    fun hideLoading() {
        loadingJob?.cancel()
        _showLoadingDialog.value = false
    }

    /**
     * Show a snackbar with the given message.
     * Emits a one-time action that will be collected by the main view.
     */
    fun showSnackbar(
        message: String,
        type: SnackbarType = SnackbarType.SUCCESS,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ) {
        _snackbarActions.tryEmit(SnackbarAction.Show(message, type, duration))
    }

    /**
     * Dismiss the currently visible snackbar.
     * Emits a one-time dismiss action.
     */
    fun dismissSnackbar() {
        _snackbarActions.tryEmit(SnackbarAction.Dismiss)
    }

    /**
     * Dispose of the manager and cancel all pending operations.
     * Useful for testing scenarios where you want to cleanly tear down the manager.
     */
    fun dispose() {
        hideLoading()
        scope.cancel()
    }

    companion object {
        private const val LOADING_DIALOG_GRACE_MS = 150L
    }
}
