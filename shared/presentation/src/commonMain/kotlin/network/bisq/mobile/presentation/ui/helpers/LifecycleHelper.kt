package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import network.bisq.mobile.presentation.ViewPresenter

/**
 * @param presenter
 * @param onExecute <optional> callback after view attached
 * @param onDispose <optional> callback before on view unnattaching
 */
@Composable
fun RememberPresenterLifecycle(presenter: ViewPresenter, onExecute: (() -> Unit)? = null, onDispose: (() -> Unit)? = null) {
    DisposableEffect(presenter) {
        presenter.onViewAttached() // Called when the view is attached
        onExecute?.let {
            onExecute()
        }

        onDispose {
            presenter.onViewUnattaching() // Called when the view is detached
            onDispose?.let {
                onDispose()
            }
        }
    }
}