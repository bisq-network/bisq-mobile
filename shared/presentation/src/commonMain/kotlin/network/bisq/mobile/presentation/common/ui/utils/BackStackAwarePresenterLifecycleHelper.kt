@file:Suppress(
    "ktlint:compose:vm-injection-check",
    "ktlint:compose:vm-forwarding-check",
    "ktlint:compose:naming-check",
)

package network.bisq.mobile.presentation.common.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.presentation.common.ui.base.ViewPresenter
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.koin.compose.getKoin

/**
 * Back-stack-aware presenter lifecycle helper.
 *
 * Unlike [RememberPresenterLifecycle] which disposes the presenter's scope every time the
 * Composable leaves composition, this helper keeps the presenter alive while its screen is
 * on the navigation back stack:
 *
 * - **First composition:** calls [ViewPresenter.onViewAttached]
 * - **Screen hidden** (navigated forward, screen goes to back stack): calls [ViewPresenter.onViewHidden].
 *   The presenter's coroutine scope stays alive — in-flight work continues.
 * - **Screen revealed** (navigated back, screen returns from back stack): calls [ViewPresenter.onViewRevealed].
 *   No re-subscription needed — the scope was never disposed.
 * - **Screen destroyed** (back stack entry popped): calls [ViewPresenter.onViewUnattaching] from
 *   the ViewModel's [ViewModel.onCleared] — full cleanup, scope disposed.
 *
 * ## How it works
 *
 * The presenter is created via Koin inside the [ViewModel] factory — runs only once per
 * NavBackStackEntry. This means:
 * - Only one presenter instance is ever created per screen (no wasted instances on recomposition)
 * - The presenter instance survives recomposition and back-stack navigation
 * - The presenter instance survives configuration changes (rotation, dark mode)
 * - The presenter is cleaned up when the NavBackStackEntry is popped (back navigation past this screen)
 *
 * The [ViewModel] is an internal implementation detail — it's just a container. The presenter
 * pattern, DI, and testing approach remain unchanged.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val presenter = RememberPresenterLifecycleBackStackAware<MyPresenter>()
 *     // ... use presenter to collect state and handle actions
 * }
 * ```
 *
 * ## Android configuration changes
 *
 * This helper also provides **configuration change survival** (rotation, dark mode, language
 * change) for free. When Android destroys and recreates the Activity, the ViewModel container
 * survives — so the presenter, its scope, and all in-flight coroutines persist across the
 * config change. The lifecycle is: `onViewHidden()` → Activity recreated → `onViewRevealed()`.
 * `onViewUnattaching()` is NOT called during config changes.
 *
 * Screens using [RememberPresenterLifecycle] do NOT survive config changes — they restart
 * from scratch (new presenter, new scope, `onViewAttached()` called again).
 *
 * ## When to use
 *
 * Use this for screens where you want the presenter to survive back navigation and/or
 * configuration changes:
 * - Wizard step screens (create offer, take offer) where going back should preserve state
 * - Tab screens that should keep their data when switching tabs
 * - Any screen with expensive initialization that shouldn't re-run on back navigation
 * - Screens that should preserve state across rotation/dark mode changes
 *
 * ## When NOT to use
 *
 * Keep using [RememberPresenterLifecycle] for:
 * - Screens with no back-stack (splash, onboarding)
 * - Screens that should always start fresh
 * - Dialog presenters
 *
 * ## Presenter requirements
 *
 * Your presenter's [ViewPresenter.onViewAttached] will only be called once (on first
 * composition). Coroutines launched there survive across back-stack navigation and config
 * changes. Override [ViewPresenter.onViewRevealed] if you need to refresh data when the
 * screen returns.
 *
 * @return The managed presenter instance — always the same instance across back-stack
 *         navigation and config changes. Use this for collecting StateFlows and invoking actions.
 */
@ExcludeFromCoverage
@Composable
inline fun <reified T : ViewPresenter> RememberPresenterLifecycleBackStackAware(): T {
    val log = getLogger("BackStackLifecycle")
    // getKoin() is @Composable — captured here in composable scope.
    // koin.get<T>() inside the viewModel factory is non-composable — safe to call there.
    val koin = getKoin()

    val holder =
        viewModel {
            PresenterHolder(koin.get<T>()).also {
                log.d { "PresenterHolder created for ${T::class.simpleName}" }
            }
        }

    @Suppress("UNCHECKED_CAST")
    val managedPresenter = holder.presenter as T

    DisposableEffect(holder) {
        try {
            if (!holder.hasBeenAttached) {
                managedPresenter.onViewAttached()
                holder.hasBeenAttached = true
                log.d { "onViewAttached (first) — ${managedPresenter::class.simpleName}" }
            } else {
                managedPresenter.onViewRevealed()
                log.d { "onViewRevealed — ${managedPresenter::class.simpleName}" }
            }
        } catch (e: Exception) {
            GenericErrorHandler.handleGenericError(
                "Error during view initialization: ${managedPresenter::class.simpleName}",
                e,
            )
        }

        onDispose {
            try {
                managedPresenter.onViewHidden()
                log.d { "onViewHidden — ${managedPresenter::class.simpleName}" }
            } catch (e: Exception) {
                GenericErrorHandler.handleGenericError(
                    "Error during view hide: ${managedPresenter::class.simpleName}",
                    e,
                )
            }
        }
    }

    return managedPresenter
}

/**
 * ViewModel that holds a presenter instance and ensures cleanup when the
 * NavBackStackEntry is popped. This is an implementation detail — not part of the public API.
 * This allows us to reuse Compose's ViewModel infrastructure to detect when a composable
 * is removed from the navigation stack.
 *
 * Public visibility is required for the inline reified [RememberPresenterLifecycleBackStackAware]
 * function to access it. Do not use directly.
 */
@ExcludeFromCoverage
class PresenterHolder(
    val presenter: ViewPresenter,
) : ViewModel() {
    private val log = getLogger("PresenterHolder")

    var hasBeenAttached: Boolean = false

    override fun onCleared() {
        if (hasBeenAttached) {
            try {
                presenter.onViewUnattaching()
                log.d { "onViewUnattaching (ViewModel cleared) — ${presenter::class.simpleName}" }
            } catch (e: Exception) {
                GenericErrorHandler.handleGenericError(
                    "Error during presenter cleanup: ${presenter::class.simpleName}",
                    e,
                )
            }
        }
        super.onCleared()
    }
}
