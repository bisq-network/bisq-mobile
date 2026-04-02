@file:Suppress("ktlint:compose:vm-injection-check", "ktlint:compose:vm-forwarding-check")

package network.bisq.mobile.presentation.common.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.presentation.common.ui.base.ViewPresenter
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler

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
 * The presenter is stored inside a [ViewModel] scoped to the current [NavBackStackEntry].
 * This means:
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
 *     val presenter: MyPresenter = koinInject()
 *     RememberPresenterLifecycleBackStackAware(presenter)
 *     // ... rest of the screen
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
 */
@Composable
fun RememberPresenterLifecycleBackStackAware(presenter: ViewPresenter) {
    val log = getLogger("BackStackLifecycle")

    // Store the presenter in a ViewModel scoped to the NavBackStackEntry.
    // The ViewModel survives recomposition, back-stack, and config changes.
    // When the NavBackStackEntry is popped, onCleared() fires → onViewUnattaching().
    // TODO: On config changes, koinInject() in the caller creates a new presenter instance that
    //  is ignored (the PresenterHolder keeps the original). This is harmless but wasteful.
    //  Could be optimized by moving koinInject() inside the viewModel factory lambda.
    //  For now its ok but might become hi-prio when we decide to support Tablets
    val holder =
        viewModel {
            PresenterHolder(presenter).also {
                log.d { "PresenterHolder created for ${presenter::class.simpleName}" }
            }
        }

    DisposableEffect(holder) {
        try {
            if (!holder.hasBeenAttached) {
                presenter.onViewAttached()
                holder.hasBeenAttached = true
                log.d { "onViewAttached (first) — ${presenter::class.simpleName}" }
            } else {
                presenter.onViewRevealed()
                log.d { "onViewRevealed — ${presenter::class.simpleName}" }
            }
        } catch (e: Exception) {
            GenericErrorHandler.handleGenericError(
                "Error during view initialization: ${presenter::class.simpleName}",
                e,
            )
        }

        onDispose {
            try {
                // The Composable is leaving composition. Since the presenter lives in the
                // ViewModel, it survives. We just notify it that the view is hidden.
                // Full cleanup (onViewUnattaching) happens in PresenterHolder.onCleared()
                // when the NavBackStackEntry is popped.
                presenter.onViewHidden()
                log.d { "onViewHidden — ${presenter::class.simpleName}" }
            } catch (e: Exception) {
                GenericErrorHandler.handleGenericError(
                    "Error during view hide: ${presenter::class.simpleName}",
                    e,
                )
            }
        }
    }
}

/**
 * Internal ViewModel that holds a presenter instance and ensures cleanup when the
 * NavBackStackEntry is popped. This is an implementation detail — not part of the public API.
 * This allow us to reuse compose viewmodel helper implementation to detect a composable being removed from the NavStack
 */
internal class PresenterHolder(
    private val presenter: ViewPresenter,
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
