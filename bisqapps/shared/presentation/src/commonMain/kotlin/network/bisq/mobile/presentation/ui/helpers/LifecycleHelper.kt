package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import network.bisq.mobile.presentation.ViewPresenter

@Composable
fun RememberPresenterLifecycle(presenter: ViewPresenter) {
    DisposableEffect(presenter) {
        presenter.onViewAttached() // Called when the view is attached

        onDispose {
            presenter.onViewUnattaching() // Called when the view is detached
        }
    }
}