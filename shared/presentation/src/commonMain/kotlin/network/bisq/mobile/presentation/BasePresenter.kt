package network.bisq.mobile.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.annotation.CallSuper
import androidx.navigation.NavHostController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.i18n.AppStrings
import network.bisq.mobile.domain.utils.Logging

/**
 * Presenter methods accesible by all views. Views should extend this interface when defining the behaviour expected for their presenter.
 */
interface ViewPresenter {

    /**
     * allows to enable/disable UI components from the presenters
     */
    val isInteractive: StateFlow<Boolean>

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

    /**
     * Navigate back in the stack
     */
    fun goBack()

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
abstract class BasePresenter(private val rootPresenter: MainPresenter?): ViewPresenter, Logging {
    protected var view: Any? = null
    // Coroutine scope for the presenter
    protected val presenterScope = CoroutineScope(Dispatchers.Main + Job())
    protected val uiScope = CoroutineScope(Dispatchers.Main)
    protected val backgroundScope = CoroutineScope(BackgroundDispatcher)

    private val dependants = if (isRoot()) mutableListOf<BasePresenter>() else null

    // Presenter is interactive by default
    private val _isInteractive = MutableStateFlow(true)
    override val isInteractive: StateFlow<Boolean> = _isInteractive
    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    override fun getSnackState(): SnackbarHostState {
        return snackbarHostState
    }

    override fun showSnackbar(message: String, isError: Boolean) {
        uiScope.launch {
            snackbarHostState.showSnackbar(message, withDismissAction = true)
        }
    }

    /**
     * @throws IllegalStateException if this presenter has no root
     * @return Nav controller for navigation from the root
     */
    protected val rootNavigator: NavHostController
        get() = (rootPresenter ?: throw IllegalStateException("This presenter has no root")).navController


    init {
        rootPresenter?.registerChild(child = this)
    }
    
    protected fun enableInteractive(enable: Boolean) {
        uiScope.launch {
            if (enable) {
                delay(250L)
            }
            _isInteractive.value = enable
        }
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

    override fun goBack() {
        try {
            log.i { "goBack defaut implementation" }
            rootNavigator.popBackStack()
        } catch (e: Exception) {
            log.e(e) { "Faled to navigate back" }
        }
    }

    @CallSuper
    override fun onViewAttached() {
        log.i { "Lifecycle: View attached to presenter" }
    }

    @CallSuper
    override fun onViewUnattaching() { }

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
    protected fun <M: BaseModel, T> stateFlowFromRepository(
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
            presenterScope.cancel()
            // copy to avoid concurrency exception - no problem with multiple on destroy calls
            dependants?.toList()?.forEach { it.onDestroy() }
        } catch (e: Exception) {
            log.e("Failed cleanup", e)
        }
    }

    private fun isRoot() = rootPresenter == null

    companion object {
        lateinit var strings: AppStrings
    }

    fun setStrings(localStrings: AppStrings) {
        strings = localStrings
    }

    open fun navigateToUrl(url: String) {
        rootPresenter?.navigateToUrl(url)
    }
}
