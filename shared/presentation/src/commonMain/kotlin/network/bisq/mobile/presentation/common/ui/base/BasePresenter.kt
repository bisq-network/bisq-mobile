package network.bisq.mobile.presentation.common.ui.base

import androidx.annotation.CallSuper
import androidx.compose.material3.SnackbarDuration
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
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
    }

    protected val navigationManager: NavigationManager by inject()

    // we use KoinComponent to avoid having to pass the manager as parameter on every single presenter
    protected val jobsManager: CoroutineJobsManager by inject()

    // For presenters we need a fresh ui scope each as otherwise navigation brings conflicts
    protected val presenterScope get() = jobsManager.getScope()

    private val dependants = if (isRoot()) mutableListOf<BasePresenter>() else null

    // Global UI manager for app-wide UI state (loading dialogs, snackbars, etc.)
    protected val globalUiManager: GlobalUiManager by inject()

    /**
     * Opt-in analytics (issue #525). KoinComponent resolution rather than ctor
     * parameter to avoid threading it through every BasePresenter subclass.
     * The service is a single binding shared across the app and is a no-op
     * when analytics is disabled at build time.
     *
     * Null-tolerant access so the existing test suites (40+ files) don't all
     * need to bind [AnalyticsService] in their Koin modules. Tests that don't
     * care about analytics tracking get a silent no-op; tests that DO care
     * (see `ScreenAnalyticsCoverageTest`, `SettingsPresenterTest`) bind a
     * mock and verify against it. Production DI modules always bind a real
     * implementation, so the null path never executes outside tests.
     *
     * `by lazy` (not `by inject` / `by getKoin().injectOrNull`) is load-
     * bearing: the latter two evaluate `getKoin()` at delegate-creation time,
     * which is at presenter constructor time. Some UI tests (e.g. dialog
     * presenters under Compose UI test harnesses) construct presenters
     * without first calling `startKoin {}`, and `getKoin()` would crash with
     * "KoinApplication has not been started". The lazy + runCatching here
     * defers resolution to first read AND swallows that case, leaving the
     * service null.
     */
    protected val analyticsService: AnalyticsService? by lazy {
        runCatching { getKoin().getOrNull<AnalyticsService>() }.getOrNull()
    }

    /**
     * Override in a subclass to opt INTO automatic screen-view tracking. Default
     * is `null` — no event is emitted. This is deliberately opt-in per presenter
     * so the privacy review surface stays small: every screen that ever emits
     * an event must be explicitly enumerated via [AnalyticsEvent.ScreenOpened].
     *
     * When non-null, [onViewAttached] emits the event through [analyticsService],
     * which is a no-op unless both the build-time AND runtime opt-in gates are
     * open.
     *
     * Visibility is `internal` rather than `protected` so the screen-coverage
     * contract test in this module can read it directly (asserting each
     * presenter returns the event declared in [AnalyticsEvent.ScreenOpened.all]).
     * Cross-module subclasses (clientApp/nodeApp) are concrete subclasses of
     * abstract presenters that already live in this module, so they don't need
     * to override this method themselves — the override lives on the abstract
     * base in `:shared:presentation`.
     */
    internal open fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened? = null

    // Add a flag to track if we've shown the exit warning
    private var exitWarningShown = false

    init {
        rootPresenter?.registerChild(child = this)
    }

    @CallSuper
    override fun onViewAttached() {
        // In bisq2, UserActivityDetected is triggered on mouse move and key press events
        // In bisq-mobile, userActivityDetected is triggered on every screen navigation,
        // which helps to reset user.publishDate.
        launchUserActivityDetection()

        // Opt-in screen-view tracking. No-op unless the subclass explicitly
        // overrides analyticsScreenEvent() AND the analytics service is
        // active (build-time + runtime gates open).
        analyticsScreenEvent()?.let { event -> analyticsService?.track(event) }
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

    private fun mainPresenterForUrlNavigation(): MainPresenter? = rootPresenter ?: (this as? MainPresenter)

    /**
     * Schedules opening [url] in the system browser. Delegates to [MainPresenter.navigateToUrlWithLauncher],
     * which handles launcher failures and exceptions (snackbar + [Boolean] result).
     * For the [Boolean] result inside a coroutine, use [navigateToUrlAwait].
     */
    open fun navigateToUrl(url: String) {
        showLoading()
        presenterScope.launch {
            try {
                mainPresenterForUrlNavigation()?.navigateToUrlWithLauncher(url)
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Same as [navigateToUrl] but suspends until the URL handoff completes and returns whether it
     * succeeded. Use from coroutines when the [Boolean] matters (e.g. web-link confirmation flow).
     */
    open suspend fun navigateToUrlAwait(url: String): Boolean {
        showLoading()
        return try {
            mainPresenterForUrlNavigation()?.navigateToUrlWithLauncher(url) ?: false
        } finally {
            hideLoading()
        }
    }

    override fun onCloseGenericErrorPanel() {
        GenericErrorHandler.clearGenericError()
    }

    override fun navigateToReportError() {
        navigateToUrl(BisqLinks.BISQ_MOBILE_GH_ISSUES)
    }

    protected fun isAtMainScreen(): Boolean = navigationManager.isAtMainScreen()

    /**
     * Navigate to given destination
     */
    protected fun navigateTo(
        destination: NavRoute,
        customSetup: (NavOptionsBuilder) -> Unit = {},
    ) {
        showLoading()
        navigationManager.navigate(
            destination,
            customSetup,
        ) {
            hideLoading()
        }
    }

    protected fun navigateBack() {
        log.d { "Navigating back" }
        showLoading()
        navigationManager.navigateBack {
            hideLoading()
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

    /**
     * Runs [block] on [presenterScope] behind [guard] using compareAndSet.
     * [guard] uses `true` = enabled; it is cleared for the duration of [block]
     * and restored on job completion when [reEnableGuardOnComplete] is true.
     *
     * @return `null` when the guard rejects the call (action already in progress),
     *   or the [Job] launched on [presenterScope] when the action was started.
     *   The caller may call [Job.cancel] to abort in-flight work; cleanup registered
     *   via [Job.invokeOnCompletion] still runs on cancellation, restoring the guard
     *   and hiding loading even when the coroutine body never started.
     */
    protected fun guardedSuspendAction(
        guard: MutableStateFlow<Boolean>,
        actionName: String,
        showLoadingOverlay: Boolean = true,
        reEnableGuardOnComplete: Boolean = true,
        block: suspend () -> Unit,
    ): Job? {
        if (!guard.compareAndSet(expect = true, update = false)) {
            log.w { "$actionName called while already in progress; ignoring" }
            return null
        }
        if (showLoadingOverlay) {
            showLoading()
        }
        val job = presenterScope.launch { block() }
        job.invokeOnCompletion {
            if (reEnableGuardOnComplete) {
                guard.value = true
            }
            if (showLoadingOverlay) {
                hideLoading()
            }
        }
        return job
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

    private fun launchUserActivityDetection() {
        presenterScope.launch {
            if (I18nSupport.isReady) { // Makes sure bundles are loaded. This fails for Splash
                rootPresenter?.userProfileServiceFacade?.userActivityDetected()
            }
        }
    }
}
