package network.bisq.mobile.presentation.common.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import network.bisq.mobile.presentation.common.ui.base.ViewPresenter
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler

/**
 *  * Keep using this composable [RememberPresenterLifecycle] for:
 *  * - Screens with no back-stack (splash, onboarding)
 *  * - Screens that should always start fresh (login, settings that read from service)
 *  * - Dialog presenters
 *
 *  Otherwise, please use [RememberPresenterLifecycleBackStackAware] for example:
 *  * - Screens with back-stack (main, trade)
 *  * - Screens that should not start fresh (trade that reads from service)
 *
 * @param presenter to remember lifecycle
 * @param onExecute <optional> callback after view attached
 * @param onDispose <optional> callback before on view unattaching
 */
@Composable
fun RememberPresenterLifecycle(
    presenter: ViewPresenter,
    onExecute: (() -> Unit)? = null,
    onDispose: (() -> Unit)? = null,
) {
    val currentOnExecute = rememberUpdatedState(onExecute)
    val currentOnDispose = rememberUpdatedState(onDispose)

    DisposableEffect(presenter) {
        try {
            presenter.onViewAttached()
            currentOnExecute.value?.invoke()
        } catch (e: Exception) {
            GenericErrorHandler.handleGenericError(
                "Error during view initialization: ${presenter::class.simpleName}",
                e,
            )
        }

        onDispose {
            try {
                presenter.onViewUnattaching()
                currentOnDispose.value?.invoke()
            } catch (e: Exception) {
                GenericErrorHandler.handleGenericError(
                    "Error during view cleanup: ${presenter::class.simpleName}",
                    e,
                )
            }
        }
    }
}
