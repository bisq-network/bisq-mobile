package network.bisq.mobile.presentation

import androidx.annotation.CallSuper
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.navigation.TabNavRoute
import network.bisq.mobile.presentation.ui.navigation.manager.NavigationManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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

    fun getSnackState(): SnackbarHostState

    fun showSnackbar(
        message: String,
        isError: Boolean = true,
        duration: SnackbarDuration = SnackbarDuration.Short,
    )

    fun dismissSnackbar()

    /**
     * @return true if user is in home tab, false otherwise
     */
    fun isAtHomeTab(): Boolean

    fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean = true,
        shouldLaunchSingleTop: Boolean = true,
        shouldRestoreState: Boolean = true
    )

    /**
     * Handle event of back navigation whilst on main tabs screen (e.g. swipes gesture)
     */
    fun onMainBackNavigation()

    /**
     * This can be used as initialization method AFTER view gets attached (so view is available)
     */
    fun onViewAttached()

    /**
     * This can be used as cleanup BEFORE unattaching a view
     */
    fun onViewUnattaching()

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
abstract class BasePresenter(private val rootPresenter: MainPresenter?) :
    ViewPresenter, KoinComponent, Logging {
    companion object {
        const val NAV_GRAPH_MOUNTING_DELAY = 100L
        const val EXIT_WARNING_TIMEOUT = 3000L
        const val SMALLEST_PERCEPTIVE_DELAY = 250L
        var isDemo = false
    }

    protected var view: Any? = null

    protected val navigationManager: NavigationManager by inject()

    // we use KoinComponent to avoid having to pass the manager as parameter on every single presenter
    protected val jobsManager: CoroutineJobsManager by inject()

    // For presenters we need a fresh ui scope each as otherwise navigation brings conflicts
    protected val presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val dependants = if (isRoot()) mutableListOf<BasePresenter>() else null

    // Presenter is interactive by default
    private val _isInteractive = MutableStateFlow(true)
    override val isInteractive: StateFlow<Boolean> get() = _isInteractive.asStateFlow()
    private val snackbarHostState: SnackbarHostState = SnackbarHostState()

    protected open val blockInteractivityOnAttached = false

    override fun getSnackState(): SnackbarHostState {
        return snackbarHostState
    }

    override fun showSnackbar(
        message: String,
        isError: Boolean,
        duration: SnackbarDuration,
    ) {
        launchUI(Dispatchers.Main) {
            snackbarHostState.showSnackbar(message, withDismissAction = true, duration = duration)
        }
    }

    override fun dismissSnackbar() {
        launchUI(Dispatchers.Main) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    override fun isSmallScreen(): Boolean {
        return rootPresenter?.isSmallScreen?.value ?: false
    }

    init {
        rootPresenter?.registerChild(child = this)
    }

    protected fun disableInteractive() {
        _isInteractive.value = false
    }

    protected fun enableInteractive() {
        launchUI {
            delay(SMALLEST_PERCEPTIVE_DELAY)
            _isInteractive.value = true
        }
    }

    override fun isIOS(): Boolean {
        val platformInfo = getPlatformInfo()
        val isIOS = platformInfo.name.lowercase().contains("ios")
        log.d { "isIOS = $isIOS" }
        return isIOS
    }

    override fun isAtHomeTab(): Boolean {
        return navigationManager.isAtHomeTab()
    }

    protected fun isAtMainScreen(): Boolean {
        return navigationManager.isAtMainScreen()
    }

    /**
     * Navigate to given destination
     */
    protected fun navigateTo(destination: NavRoute, customSetup: (NavOptionsBuilder) -> Unit = {}) {
        disableInteractive()
        navigationManager.navigate(
            destination,
            customSetup
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
        shouldSaveState: Boolean = false
    ) {
        navigationManager.navigateBackTo(
            destination,
            shouldInclusive,
            shouldSaveState
        )
    }

    /**
     * Navigates to the given tab route inside the main presentation, with default parameters.
     */
    override fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean,
        shouldLaunchSingleTop: Boolean,
        shouldRestoreState: Boolean
    ) {
        navigationManager.navigateToTab(
            destination,
            saveStateOnPopUp,
            shouldLaunchSingleTop,
            shouldRestoreState
        )
    }

    /**
     * Navigates to Offerbook tab.
     * Called from Create offer, Take offer flow to close the work flow.
     */
    protected fun navigateToOfferbookTab() {
        navigateBackTo(NavRoute.TabContainer)
        navigateToTab(NavRoute.TabOfferbookMarket)
    }

    // Add a flag to track if we've shown the exit warning
    private var exitWarningShown = false

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
                    launchUI {
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
                    shouldRestoreState = false
                )
            }

            else -> {
                // Reset the exit warning for normal back navigation
                exitWarningShown = false
                navigateBack()
            }
        }
    }

    @CallSuper
    override fun onViewAttached() {
        log.i { "Lifecycle: View ${if (view != null) view!!::class.simpleName else ""} attached to presenter ${this::class.simpleName}" }
        if (blockInteractivityOnAttached) {
            blockInteractivityForBriefMoment()
        } else {
            enableInteractive()
        }

        // In bisq2, UserActivityDetected is triggered on mouse move and key press events
        // In bisq-mobile, userActivityDetected is triggered on every screen navigation,
        // which helps to reset user.publishDate.
        launchUI {
            if (I18nSupport.isReady) { // Makes sure bundles are loaded. This fails for Splash
                rootPresenter?.userProfileServiceFacade?.userActivityDetected()
            }
        }
    }

    @CallSuper
    override fun onViewUnattaching() {
        // Presenter level support for auto disposal
        CoroutineScope(IODispatcher).launch { jobsManager.dispose() }
    }

    @CallSuper
    override fun onDestroying() {
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
        } finally {
//          we can't get read of the link here since link is done at construction only
//            and we are using singletons
//            rootPresenter?.unregisterChild(this)
        }
    }

    fun attachView(view: Any) {
        // at the moment the attach view is with the activity/ main view in ios
        // unless we change this there is no point in sharing with dependents
        this.view = view
        log.i { "Lifecycle: Main View attached to Main Presenter" }
    }

    fun detachView() {
        onViewUnattaching()
        this.view = null
        log.i { "Lifecycle: View Dettached from Presenter" }
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

    /**
     * Pass through function that allows you to link the requested type the view is asking for in its view presenter interface
     * with the field in the domain model of the repository.
     *
     * @param M base model from where to extract the requested type T
     * @param T the requested view type T
     * @param repositoryFlow
     * @param transform the transformation function from the
     * @param initialValue
     * @param scope defaults to Coroutine.Main (view thread)
     * @param started defaults to Lazy loading
     */
    protected fun <M : BaseModel, T> stateFlowFromRepository(
        repositoryFlow: StateFlow<M?>,
        transform: (M?) -> T,
        initialValue: T,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        started: SharingStarted = SharingStarted.Lazily
    ): StateFlow<T> {
        return repositoryFlow
            .map { transform(it) }
            .stateIn(
                scope = scope,
                started = started,
                initialValue = initialValue
            )
    }

    private fun cleanup() {
        try {
            runBlocking {
                jobsManager.dispose()
            }
            // copy to avoid concurrency exception - no problem with multiple on destroy calls
            dependants?.toList()?.forEach { it.onDestroy() }
        } catch (e: Exception) {
            log.e("Failed cleanup", e)
        }
    }

    private fun isRoot() = rootPresenter == null

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
            else -> log.w {
                "Invalid type. We do not have set the rootPresenter and expect to be the " +
                        "MainPresenter which implements AppPresenter"
            }
        }
    }

    fun terminateApp() {
        when {
            rootPresenter is AppPresenter -> rootPresenter.onTerminateApp()
            this is AppPresenter -> onTerminateApp()
            else -> log.w {
                "Invalid type. We do not have set the rootPresenter and expect to be the " +
                        "MainPresenter which implements AppPresenter"
            }
        }
    }

    open fun navigateToUrl(url: String) {
        disableInteractive()
        rootPresenter?.navigateToUrl(url)
        enableInteractive()
    }

    override fun onCloseGenericErrorPanel() {
        GenericErrorHandler.clearGenericError()
    }

    override fun navigateToReportError() {
        navigateToUrl(BisqLinks.BISQ_MOBILE_GH_ISSUES)
    }

    protected open fun isDevMode(): Boolean {
        return rootPresenter?.isDevMode() ?: false
    }

    override fun isDemo(): Boolean = rootPresenter?.isDemo() ?: false

    // Presenter-level helper methods for launching coroutines with the jobsManager
    protected fun launchUI(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return jobsManager.launchUI(context, block)
    }

    protected fun launchIO(block: suspend CoroutineScope.() -> Unit): Job {
        return jobsManager.launchIO(block)
    }

    protected fun <T> collectUI(flow: Flow<T>, collector: suspend (T) -> Unit): Job {
        return jobsManager.collectUI(flow, collector)
    }

    protected fun <T> collectIO(flow: Flow<T>, collector: suspend (T) -> Unit): Job {
        return jobsManager.collectIO(flow, collector)
    }

    private fun blockInteractivityForBriefMoment() {
        disableInteractive()
        enableInteractive()
    }
}
