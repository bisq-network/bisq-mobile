package network.bisq.mobile.presentation.common.ui.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Global UI state manager for app-wide UI elements like loading dialogs.
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
     * Dispose of the manager and cancel all pending operations.
     * Useful for testing scenarios where you want to cleanly tear down the manager.
     */
    fun dispose() {
        loadingJob?.cancel()
        scope.cancel()
    }

    companion object {
        private const val LOADING_DIALOG_GRACE_MS = 150L
    }
}
