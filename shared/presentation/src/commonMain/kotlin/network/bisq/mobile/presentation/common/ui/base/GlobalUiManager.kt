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
 * Enum representing the position of the snackbar on screen
 */
enum class SnackbarPosition {
    TOP,
    BOTTOM,
}

/**
 * Sealed class representing all snackbar actions for the unified SharedFlow
 */
sealed class SnackbarAction {
    data class Show(
        val message: String,
        val type: SnackbarType = SnackbarType.SUCCESS,
        val duration: SnackbarDuration = SnackbarDuration.Short,
        val position: SnackbarPosition = SnackbarPosition.BOTTOM,
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

    private val _isLoadingBlocking = MutableStateFlow(false)
    val isLoadingBlocking: StateFlow<Boolean> = _isLoadingBlocking.asStateFlow()

    private val _showLoadingDialog = MutableStateFlow(false)
    val showLoadingDialog: StateFlow<Boolean> = _showLoadingDialog.asStateFlow()

    private var showLoadingJob: Job? = null
    private var hideLoadingJob: Job? = null

    // Snackbar actions as SharedFlow for one-time events (buffer holds 1, drops oldest on overflow)
    private val _snackbarActions =
        MutableSharedFlow<SnackbarAction>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val snackbarActions: SharedFlow<SnackbarAction> = _snackbarActions.asSharedFlow()

    /**
     * Schedule showing a loading dialog after a grace delay.
     * Immediately blocks screen interaction. If the operation completes before the delay expires,
     * the dialog never appears (avoiding flicker).
     * Call [scheduleHideLoading] when the operation completes to cancel the scheduled show and hide the dialog.
     */
    fun scheduleShowLoading() {
        hideLoadingJob?.cancel()
        hideLoadingJob = null
        _isLoadingBlocking.value = true
        showLoadingJob?.cancel()
        showLoadingJob =
            scope.launch {
                delay(LOADING_DIALOG_GRACE_MS)
                _showLoadingDialog.value = true
            }
    }

    /**
     * Schedule hiding the loading dialog and blocking overlay after a grace delay.
     * Cancels any scheduled show immediately, but keeps blocking interaction for the grace period
     * so navigation can complete before the user can tap underlying UI.
     */
    fun scheduleHideLoading() {
        showLoadingJob?.cancel()
        showLoadingJob = null
        hideLoadingJob?.cancel()
        hideLoadingJob =
            scope.launch {
                delay(LOADING_DIALOG_GRACE_MS)
                _isLoadingBlocking.value = false
                _showLoadingDialog.value = false
            }
    }

    /**
     * Show a snackbar with the given message.
     * Emits a one-time action that will be collected by the main view.
     */
    fun showSnackbar(
        message: String,
        type: SnackbarType = SnackbarType.SUCCESS,
        duration: SnackbarDuration = SnackbarDuration.Short,
        position: SnackbarPosition = SnackbarPosition.BOTTOM,
    ) {
        _snackbarActions.tryEmit(SnackbarAction.Show(message, type, duration, position))
    }

    /**
     * Dismiss the currently visible snackbar.
     * Emits a one-time dismiss action.
     */
    fun dismissSnackbar() {
        _snackbarActions.tryEmit(SnackbarAction.Dismiss)
    }

    /**
     * Reset transient loading state without tearing down the scope.
     * Cancels any pending show/hide jobs and clears both the blocking overlay and the dialog.
     *
     * Use this on presenter teardown: [GlobalUiManager] is an app-lifetime singleton that outlives
     * any individual presenter, so its [scope] must stay usable for the next session. Cancelling the
     * scope here (via [dispose]) would permanently break [scheduleShowLoading]/[scheduleHideLoading]
     * and leave [isLoadingBlocking] stuck `true` after an Activity is recreated over a surviving
     * process (e.g. foreground-service kill + immediate restart), freezing the UI.
     */
    fun reset() {
        clearLoadingStateNow()
    }

    /**
     * Dispose of the manager and cancel all pending operations, including the underlying [scope].
     * Only for teardown/testing where the instance is discarded — the scope is not recreated, so a
     * disposed instance can no longer schedule loading. For production presenter teardown use [reset].
     */
    fun dispose() {
        clearLoadingStateNow()
        scope.cancel()
    }

    private fun clearLoadingStateNow() {
        showLoadingJob?.cancel()
        showLoadingJob = null
        hideLoadingJob?.cancel()
        hideLoadingJob = null
        _isLoadingBlocking.value = false
        _showLoadingDialog.value = false
    }

    companion object {
        private const val LOADING_DIALOG_GRACE_MS = 150L
    }
}
