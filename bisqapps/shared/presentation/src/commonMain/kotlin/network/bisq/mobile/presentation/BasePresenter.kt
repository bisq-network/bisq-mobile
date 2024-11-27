package network.bisq.mobile.presentation

import androidx.annotation.CallSuper
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.utils.Logging


/**
 * Presenter methods accesible by all views. Views should extend this interface when defining the behaviour expected for their presenter.
 */
interface ViewPresenter {
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

    private val dependants = if (isRoot()) mutableListOf<BasePresenter>() else null

    /**
     * @throws IllegalStateException if this presenter has no root
     * @return Nav controller for navigation from the root
     */
    protected val rootNavigator: NavHostController
        get() = (rootPresenter ?: throw IllegalStateException("This presenter has no root")).navController


    init {
        rootPresenter?.registerChild(child = this)
    }

    @CallSuper
    override fun onViewAttached() { }

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
            rootPresenter?.unregisterChild(this)
        }
    }

    fun attachView(view: Any) {
        // at the moment the attach view is with the activity/ main view in ios
        // unless we change this there is no point in sharing with dependents
        this.view = view
        log.i { "Lifecycle: View Attached to Presenter" }
        onViewAttached()
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
}
