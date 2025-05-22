package network.bisq.mobile.presentation

import androidx.annotation.CallSuper
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.ui.navigation.Routes
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

    /**
     * @return root navigation controller
     */
    fun getRootNavController(): NavHostController

    /**
     * @return main app tab nav controller
     */
    fun getRootTabNavController(): NavHostController

    fun getSnackState(): SnackbarHostState

    fun showSnackbar(message: String, isError: Boolean = true)
    fun dismissSnackbar()

    /**
     * @return true if user is in home tab, false otherwise
     */
    fun isAtHome(): Boolean

    fun navigateToTab(
        destination: Routes,
        saveStateOnPopUp: Boolean = true,
        shouldLaunchSingleTop: Boolean = true,
        shouldRestoreState: Boolean = true
    )

    /**
     * Navigate back in the stack
     * **CAUTION** this irreversibly removes backstack history
     */
    fun goBack(): Boolean

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
abstract class BasePresenter(private val rootPresenter: MainPresenter?) : ViewPresenter, KoinComponent, Logging {
    companion object {
        const val DEFAULT_DELAY = 250L
        const val EXIT_WARNING_TIMEOUT = 3000L
        var isDemo = false
    }

    protected var view: Any? = null

    // we use KoinComponent to avoid having to pass the manager as parameter on efery single presenter
    protected val jobsManager: CoroutineJobsManager by inject()
    
    // For backward compatibility - these will delegate to the jobsManager
    protected val presenterScope: CoroutineScope
        get() = jobsManager.getUIScope()
    
    protected val ioScope: CoroutineScope
        get() = jobsManager.getIOScope()

    private val dependants = if (isRoot()) mutableListOf<BasePresenter>() else null

    // Presenter is interactive by default
    private val _isInteractive = MutableStateFlow(true)
    override val isInteractive: StateFlow<Boolean> = _isInteractive
    private val snackbarHostState: SnackbarHostState = SnackbarHostState()

    override fun getSnackState(): SnackbarHostState {
        return snackbarHostState
    }

    override fun showSnackbar(message: String, isError: Boolean) {
        launchUI(Dispatchers.Main) {
            snackbarHostState.showSnackbar(message, withDismissAction = true)
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

    /**
     * @throws IllegalStateException if this presenter has no root
     * @return Nav controller for navigation from the root
     */
    private val rootNavigator: NavHostController
        get() = (rootPresenter ?: throw IllegalStateException("This presenter has no root")).navController


    init {
        rootPresenter?.registerChild(child = this)
    }

    protected fun disableInteractive() {
        _isInteractive.value = false
    }

    protected fun enableInteractive() {
        launchUI {
            delay(250L)
            _isInteractive.value = true
        }
    }

    override fun isIOS(): Boolean {
        val platformInfo = getPlatformInfo()
        val isIOS = platformInfo.name.lowercase().contains("ios")
        log.d { "isIOS = $isIOS" }
        return isIOS
    }

    /**
     * Default implementation assumes is a child presenter
     */
    override fun getRootNavController(): NavHostController {
        if (isRoot()) {
            throw IllegalStateException("You need to redefine this method in your root presenter implementation")
        }
        return rootPresenter!!.getRootNavController()
    }

    /**
     * Default implementation assumes is a child presenter
     */
    override fun getRootTabNavController(): NavHostController {
        if (isRoot()) {
            throw IllegalStateException("You need to redefine this method in your root presenter implementation")
        }
        return rootPresenter!!.getRootTabNavController()
    }

    protected open fun pushNotification(title: String, content: String) {
        if (isRoot()) {
            throw IllegalStateException("You need to redefine this method in your root presenter implementation")
        }
        rootPresenter!!.pushNotification(title, content)
    }

    override fun isAtHome(): Boolean {
        val currentTab = getRootTabNavController().currentBackStackEntry?.destination?.route
        log.d { "Current tab $currentTab" }
        return isAtMainScreen() && (currentTab == null || currentTab == Routes.TabHome.name)
    }

    protected fun isAtMainScreen(): Boolean {
        val currentScreen = getRootNavController().currentBackStackEntry?.destination?.route
        log.d { "Current screen $currentScreen" }
        return (currentScreen == null || currentScreen == Routes.TabContainer.name)
    }

    /**
     * Navigate to given destination
     */
    protected fun navigateTo(destination: Routes, customSetup: (NavOptionsBuilder) -> Unit = {}) {
        disableInteractive()
        launchUI(Dispatchers.Main) {
            try {
                rootNavigator.navigate(destination.name) {
                    customSetup(this)
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to navigate to ${destination.name}" }
            } finally {
                enableInteractive()
            }
        }
    }

    protected fun navigateBack(): Boolean {
        log.d { "Navigating back" }
        return goBack()
    }

    /**
     * Back navigation popping back stack
     */
    protected fun navigateBackTo(destination: Routes, shouldInclusive: Boolean = false, shouldSaveState: Boolean = false) {
        launchUI(Dispatchers.Main) {
            rootNavigator.popBackStack(destination.name, inclusive = shouldInclusive, saveState = shouldSaveState)
        }
    }

    /**
     * Navigates to the given tab route inside the main presentation, with default parameters.
     */
    override fun navigateToTab(
        destination: Routes,
        saveStateOnPopUp: Boolean,
        shouldLaunchSingleTop: Boolean,
        shouldRestoreState: Boolean
    ) {
        log.d { "Navigating to tab ${destination.name} " }
        launchUI {
            getRootTabNavController().navigate(destination.name) {
                getRootTabNavController().graph.startDestinationRoute?.let { route ->
                    popUpTo(route) {
                        saveState = saveStateOnPopUp
                    }
                }
                launchSingleTop = shouldLaunchSingleTop
                restoreState = shouldRestoreState
            }
        }
    }

    // Add a flag to track if we've shown the exit warning
    private var exitWarningShown = false

    override fun onMainBackNavigation() {
        when {
            isAtHome() -> {
                if (exitWarningShown) {
                    rootPresenter?.exitApp()
                    exitWarningShown = false // Reset after action
                } else {
                    // Show warning first time
                    showSnackbar("Press/Swipe back again to exit")
                    exitWarningShown = true

                    // Set a timer to reset the warning state after a few seconds
                    launchUI {
                        delay(EXIT_WARNING_TIMEOUT) // 3 seconds timeout for exit warning
                        exitWarningShown = false
                    }
                }
            }

            isAtMainScreen() -> {
                // Reset the exit warning when navigating to home
                exitWarningShown = false
                navigateToTab(Routes.TabHome, saveStateOnPopUp = true, shouldLaunchSingleTop = true, shouldRestoreState = false)
            }

            else -> {
                // Reset the exit warning for normal back navigation
                exitWarningShown = false
                goBack()
            }
        }
    }

    override fun goBack(): Boolean {
        disableInteractive()
        var wentBack = false
        launchUI(Dispatchers.Main) {
            try {
                log.i { "goBack default implementation" }
                if (isIOS()) {
                    // TODO this is still not working on iOS, it could be because of the different way it handles the Dispatchers.Main coroutine
                    // making this a suspend fun and using withContext() could fix it
                    if (rootNavigator.currentBackStack.value.size > 1) {
                        wentBack = rootNavigator.popBackStack()
                    } else {
                        rootPresenter?.exitApp()
                    }
                } else {
                    wentBack = rootNavigator.popBackStack()
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to navigate back" }
            } finally {
                enableInteractive()
            }
        }
        return wentBack
    }

    @CallSuper
    override fun onViewAttached() {
        log.i { "Lifecycle: View ${if (view != null) view!!::class.simpleName else ""} attached to presenter ${this::class.simpleName}" }
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

    // TODO we need to test to find what are exactly the best places to register/unregister
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

    fun exitApp() {
        if (isRoot()) {
            exitApp(view)
        }
    }
    open fun navigateToUrl(url: String) {
        rootPresenter?.navigateToUrl(url)
    }

    override fun onCloseGenericErrorPanel() {
        GenericErrorHandler.clearGenericError()
    }

    override fun navigateToReportError() {
        disableInteractive()
        navigateToUrl("https://github.com/bisq-network/bisq-mobile/issues")
        enableInteractive()
    }

    override fun isDemo(): Boolean = rootPresenter?.isDemo() ?: false

    // Presenter-level helper methods for launching coroutines with the jobsManager
    protected fun launchUI(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit): Job {
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
}
