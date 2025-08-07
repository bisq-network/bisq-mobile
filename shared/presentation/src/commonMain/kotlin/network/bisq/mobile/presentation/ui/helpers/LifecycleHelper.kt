package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler

/**
 * @param presenter
 * @param onExecute <optional> callback after view attached
 * @param onDispose <optional> callback before on view unnattaching
 */
@Composable
fun RememberPresenterLifecycle(presenter: ViewPresenter, onExecute: (() -> Unit)? = null, onDispose: (() -> Unit)? = null) {
    DisposableEffect(presenter) {
        try {
            presenter.onViewAttached() // Called when the view is attached
            onExecute?.let {
                onExecute()
            }
        } catch (e: Exception) {
            // Handle the error gracefully without breaking Compose
            GenericErrorHandler.handleGenericError(
                "Error during view initialization: ${presenter::class.simpleName}",
                e
            )
        }

        onDispose {
            try {
                presenter.onViewUnattaching() // Called when the view is detached
                onDispose?.let {
                    onDispose()
                }
            } catch (e: Exception) {
                // Handle disposal errors gracefully
                GenericErrorHandler.handleGenericError(
                    "Error during view cleanup: ${presenter::class.simpleName}",
                    e
                )
            }
        }
    }
}