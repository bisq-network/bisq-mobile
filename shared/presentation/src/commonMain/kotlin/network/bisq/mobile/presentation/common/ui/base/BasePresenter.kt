package network.bisq.mobile.presentation.common.ui.base

import androidx.annotation.CallSuper
import androidx.compose.material3.SnackbarDuration
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.moveAppToBackground
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.main.AppPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Presenter methods accesible by all views. Views should extend this interface when defining the behaviour expected for their presenter.
 */
interface ViewPresenter {
    /**
     * allows to enable/disable UI components from the presenters
     */
    val isInteractive: StateFlow<Boolean>

    fun isDemo(): Boolean

    fun isSmallScreen(): Boolean

    fun onCloseGenericErrorPanel()

    fun navigateToReportError()

    fun isIOS(): Boolean

    fun showSnackbar(
        message: String,
        type: SnackbarType = SnackbarType.SUCCESS,
        position: SnackbarPosition = SnackbarPosition.BOTTOM,
        duration: SnackbarDuration = SnackbarDuration.Short,
    )

    /**
     * @return true if user is in home tab, false otherwise
     */
    fun isAtHomeTab(): Boolean

    fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean = true,
        shouldLaunchSingleTop: Boolean = true,
        shouldRestoreState: Boolean = true,
    )

    /**
     * Handle event of back navigation whilst on main tabs screen (e.g. swipes gesture)
     */
    fun onMainBackNavigation()

    /**
     * This can be used as initialization method AFTER view gets attached (so view is available)
     *
     * When combined with [network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware]
     * this method  will only be called only once whilst the associated view remains in the stack (on first composition).
     * Coroutines launched there survive across back-stack navigation.
     * Override [ViewPresenter.onViewRevealed] if you need to refresh data when the screen returns.
     *
     * Otherwise (using [network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle]), this
     * method is called on every render of the associated view.
     */
    fun onViewAttached()

    /**
     * This can be used as cleanup BEFORE unattaching a view
     */
    fun onViewUnattaching()

    /**
     * Called when the view is no longer visible but is still on the navigation back stack.
     * The presenter's scope stays alive — coroutines continue running.
     * Only called when using [RememberPresenterLifecycleBackStackAware].
     * With [RememberPresenterLifecycle], this is never called — [onViewUnattaching] fires instead.
     */
    @ExcludeFromCoverage
    fun onViewHidden() {}

    /**
     * Called when the view becomes visible again after being on the back stack.
     * The presenter's scope is still alive from the original [onViewAttached] — no re-subscription needed.
     * Only called when using [RememberPresenterLifecycleBackStackAware].
     */
    @ExcludeFromCoverage
    fun onViewRevealed() {}

    /**
     * This can be used to do cleanup when the view is getting destroyed
     * Base Presenter corouting scope gets cancelled right before this method is called
     */
    fun onDestroying()
}

/**
 * Presenter for any type of view.
 * The view should define its own interface that the child presenter should implement as well, but
 * this class provide generic useful and common behaviour for presenters
 *
 * Base class allows to have a tree hierarchy of presenters. If the rootPresenter is null, this presenter acts as root
 * if root present is passed, this present attach itself to the root to get updates (consequently its dependants will be always empty
 */
abstract class BasePresenter(
    private val rootPresenter: MainPresenter?,
) : ViewPresenter,
    KoinComponent,
    Logging {
    companion object {
        const val EXIT_WARNING_TIMEOUT = 3000L
        const val SMALLEST_PERCEPTIVE_DELAY = 250L
    }

    protected val navigationManager: NavigationManager by inject()

    // we use KoinComponent to avoid having to pass the manager as parameter on every single presenter
    protected val jobsManager: CoroutineJobsManager by inject()

    // For presenters we need a fresh ui scope each as otherwise navigation brings conflicts
    protected val presenterScope get() = jobsManager.getScope()

    private val dependants = if (isRoot()) mutableListOf<BasePresenter>() else null

    /**
     * override in your presenter if you want to block interactivity on view attached
     */
    protected open val blockInteractivityOnAttached = false

    // Presenter is interactive by default
    private val _isInteractive = MutableStateFlow(true)
    override val isInteractive: StateFlow<Boolean> = _isInteractive.asStateFlow()

    // Global UI manager for app-wide UI state (loading dialogs, snackbars, etc.)
    protected val globalUiManager: GlobalUiManager by inject()

    // Add a flag to track if we've shown the exit warning
    private var exitWarningShown = false

    init {
        rootPresenter?.registerChild(child = this)
    }

    @CallSuper
    override fun onViewAttached() {
        if (blockInteractivityOnAttached) {
            blockInteractivityForBriefMoment()
        } else {
            enableInteractive()
        }

        // In bisq2, UserActivityDetected is triggered on mouse move and key press events
        // In bisq-mobile, userActivityDetected is triggered on every screen navigation,
        // which helps to reset user.publishDate.
        launchUserActivityDetection()
    }

    @CallSuper
    override fun onViewUnattaching() {
        // Cancel any pending global loading dialog to prevent stuck overlays
        hideLoading()
        // Dispose presenterScope via a separate unmanaged scope. We intentionally do NOT use
        // presenterScope here because we are cancelling it — launching on a scope being cancelled
        // would be a no-op. The fire-and-forget CoroutineScope(Main) avoids iOS CA Fence hangs
        // that runBlocking caused during view teardown.
        CoroutineScope(Dispatchers.Main).launch { jobsManager.dispose() }
        // Unregister from parent to prevent memory leak with factory presenters
        rootPresenter?.unregisterChild(this)
    }

    @CallSuper
    override fun onViewHidden() {
        // View is no longer visible but still on the back stack.
        // Cancel loading overlays but keep the scope alive.
        hideLoading()
        log.d { "onViewHidden — scope alive, coroutines continue" }
    }

    @CallSuper
    override fun onViewRevealed() {
        // View is visible again after being on the back stack.
        // Scope is still alive — just re-enable interactivity.
        if (blockInteractivityOnAttached) {
            blockInteractivityForBriefMoment()
        } else {
            enableInteractive()
        }
        log.d { "onViewRevealed — scope still alive" }
    }

    @CallSuper
    override fun onDestroying() {
        // Cancel any pending global loading dialog to prevent stuck overlays
        hideLoading()
        // default impl
        log.i { "onDestroying" }
    }

    @CallSuper
    open fun onStart() {
        log.i { "Lifecycle: START" }
        this.dependants?.forEach { it.onStart() }
    }

    @CallSuper
    open fun onResume() {
        log.i { "Lifecycle: RESUME" }
        this.dependants?.forEach { it.onResume() }
    }

    @CallSuper
    open fun onPause() {
        log.i { "Lifecycle: PAUSE" }
        this.dependants?.forEach { it.onPause() }
    }

    @CallSuper
    open fun onStop() {
        log.i { "Lifecycle: STOP" }
        this.dependants?.forEach { it.onStop() }
    }

    fun onDestroy() {
        try {
            log.i { "Lifecycle: DESTROY" }
            cleanup()
            onDestroying()
        } catch (e: Exception) {
            log.e("Custom cleanup failed", e)
        }
    }

    override fun showSnackbar(
        message: String,
        type: SnackbarType,
        position: SnackbarPosition,
        duration: SnackbarDuration,
    ) {
        globalUiManager.showSnackbar(message, type, duration, position)
    }

    override fun isSmallScreen(): Boolean = rootPresenter?.isSmallScreen?.value ?: false

    override fun isIOS(): Boolean = getPlatformInfo().type == PlatformType.IOS

    /**
     * Navigates to the given tab route inside the main presentation, with default parameters.
     */
    override fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean,
        shouldLaunchSingleTop: Boolean,
        shouldRestoreState: Boolean,
    ) {
        navigationManager.navigateToTab(
            destination,
            saveStateOnPopUp,
            shouldLaunchSingleTop,
            shouldRestoreState,
        )
    }

    override fun isAtHomeTab(): Boolean = navigationManager.isAtHomeTab()

    fun moveAppToBackground() {
        if (rootPresenter == null && this is MainPresenter) {
            moveAppToBackground(view)
        } else {
            rootPresenter?.moveAppToBackground()
        }
    }

    fun restartApp() {
        when {
            rootPresenter is AppPresenter -> rootPresenter.onRestartApp()
            this is AppPresenter -> onRestartApp()
            else ->
                log.w {
                    "Invalid type. We do not have set the rootPresenter and expect to be the " +
                        "MainPresenter which implements AppPresenter"
                }
        }
    }

    fun terminateApp() {
        when {
            rootPresenter is AppPresenter -> rootPresenter.onTerminateApp()
            this is AppPresenter -> onTerminateApp()
            else ->
                log.w {
                    "Invalid type. We do not have set the rootPresenter and expect to be the " +
                        "MainPresenter which implements AppPresenter"
                }
        }
    }

    open fun navigateToUrl(url: String): Boolean {
        if (!_isInteractive.value) return false
        disableInteractive()
        return try {
            rootPresenter?.navigateToUrl(url) ?: false
        } catch (e: Exception) {
            log.e(e) { "Failed to navigate to URL: $url" }
            false
        } finally {
            enableInteractive() // re-enables after 250ms delay — prevents rapid double-taps
        }
    }

    override fun onCloseGenericErrorPanel() {
        GenericErrorHandler.clearGenericError()
    }

    override fun navigateToReportError() {
        val isOpened = navigateToUrl(BisqLinks.BISQ_MOBILE_GH_ISSUES)
        if (!isOpened) {
            showSnackbar("mobile.error.cannotOpenUrl".i18n(), SnackbarType.ERROR)
        }
    }

    protected fun isAtMainScreen(): Boolean = navigationManager.isAtMainScreen()

    /**
     * Navigate to given destination
     */
    protected fun navigateTo(
        destination: NavRoute,
        customSetup: (NavOptionsBuilder) -> Unit = {},
    ) {
        disableInteractive()
        navigationManager.navigate(
            destination,
            customSetup,
        ) {
            enableInteractive()
        }
    }

    protected fun navigateBack() {
        log.d { "Navigating back" }
        disableInteractive()
        navigationManager.navigateBack {
            enableInteractive()
        }
    }

    /**
     * Back navigation popping back stack
     */
    protected fun navigateBackTo(
        destination: NavRoute,
        shouldInclusive: Boolean = false,
        shouldSaveState: Boolean = false,
    ) {
        navigationManager.navigateBackTo(
            destination,
            shouldInclusive,
            shouldSaveState,
        )
    }

    override fun onMainBackNavigation() {
        when {
            isAtHomeTab() -> {
                if (exitWarningShown) {
                    moveAppToBackground()
                    exitWarningShown = false // Reset after action
                } else {
                    // Show warning first time
                    showSnackbar("mobile.base.swipeBackToExit".i18n())
                    exitWarningShown = true

                    // Set a timer to reset the warning state after a few seconds
                    presenterScope.launch {
                        delay(EXIT_WARNING_TIMEOUT) // 3 seconds timeout for exit warning
                        exitWarningShown = false
                    }
                }
            }

            navigationManager.isAtMainScreen() -> {
                // Reset the exit warning when navigating to home
                exitWarningShown = false
                navigateToTab(
                    destination = NavRoute.TabHome,
                    saveStateOnPopUp = true,
                    shouldLaunchSingleTop = true,
                    shouldRestoreState = false,
                )
            }

            else -> {
                // Reset the exit warning for normal back navigation
                exitWarningShown = false
                navigateBack()
            }
        }
    }

    override fun isDemo(): Boolean = rootPresenter?.isDemo() ?: false

    /**
     * Enable interactive state with a small delay to avoid flicker.
     * Link your UI to this state to disable user interactions.
     */
    protected fun enableInteractive() {
        presenterScope.launch {
            delay(SMALLEST_PERCEPTIVE_DELAY)
            _isInteractive.value = true
        }
    }

    /**
     * Disable interactive state immediately.
     * Link your UI to this state to disable user interactions.
     */
    protected fun disableInteractive() {
        _isInteractive.value = false
    }

    /**
     * Schedule showing a loading dialog after a grace delay.
     * If the operation completes before the delay expires, the dialog never appears (avoiding flicker).
     * Call hideLoading() when the operation completes to cancel the scheduled show and hide the dialog.
     * Delegates to GlobalUiManager for app-level loading dialog management.
     */
    protected fun showLoading() {
        globalUiManager.scheduleShowLoading()
    }

    /**
     * Hide the loading dialog and cancel any scheduled show.
     * Delegates to GlobalUiManager for app-level loading dialog management.
     */
    protected fun hideLoading() {
        globalUiManager.hideLoading()
    }

    protected fun registerChild(child: BasePresenter) {
        if (!isRoot()) {
            throw IllegalStateException("You can't register to a non root presenter")
        }
        this.dependants!!.add(child)
    }

    protected fun unregisterChild(child: BasePresenter) {
        if (!isRoot()) {
            throw IllegalStateException("You can't unregister from a non root presenter")
        }
        this.dependants!!.remove(child)
    }

    protected open fun isDevMode(): Boolean = rootPresenter?.isDevMode() ?: false

    /**
     * Handles common errors by showing timeout or generic snackbars.
     */
    protected fun handleError(
        exception: Throwable,
        defaultMessage: String = "mobile.error.generic".i18n(),
        position: SnackbarPosition = SnackbarPosition.BOTTOM,
        customHandler: ((Throwable) -> Boolean)? = null,
    ) {
        log.e(exception) { "Network error: ${exception.message}" }
        val handled = customHandler?.invoke(exception) == true
        if (handled) return

        val errorMessage =
            if (exception is TimeoutCancellationException) {
                "mobile.error.requestTimedOut".i18n()
            } else {
                defaultMessage
            }
        showSnackbar(errorMessage, SnackbarType.ERROR, position)
    }

    private fun cleanup() {
        try {
            // Cancel scope synchronously. scope.cancel() is non-blocking so this
            // doesn't cause the iOS CA Fence hangs that runBlocking did.
            // No scope recreation needed — presenter is being destroyed.
            runCatching { presenterScope.cancel() }
            // copy to avoid concurrency exception - no problem with multiple on destroy calls
            dependants?.toList()?.forEach { it.onDestroy() }
        } catch (e: Exception) {
            log.e("Failed cleanup", e)
        }
    }

    private fun isRoot() = rootPresenter == null

    private fun blockInteractivityForBriefMoment() {
        disableInteractive()
        enableInteractive()
    }

    private fun launchUserActivityDetection() {
        presenterScope.launch {
            if (I18nSupport.isReady) { // Makes sure bundles are loaded. This fails for Splash
                rootPresenter?.userProfileServiceFacade?.userActivityDetected()
            }
        }
    }
}
