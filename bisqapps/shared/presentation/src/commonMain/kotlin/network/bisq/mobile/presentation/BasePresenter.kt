package network.bisq.mobile.presentation

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * Presenter for any type of view.
 * The view should define its own interface that the child presenter should implement as well, but
 * this class provide generic useful and common behaviour for presenters
 *
 * Base class allows to have a tree hierarchy of presenters. If the rootPresenter is null, this presenter acts as root
 * if root present is passed, this present attach itself to the root to get updates (consequently its dependants will be always empty
 */
abstract class BasePresenter(private val rootPresenter: MainPresenter?) {
    protected var view: Any? = null
    private val log = Logger.withTag("BasePresenter")
    // Coroutine scope for the presenter
    protected val presenterScope = CoroutineScope(Dispatchers.Main + Job())

    private val dependants = if (isRoot()) mutableListOf<BasePresenter>() else null

    init {
        rootPresenter?.registerChild(child = this)
    }

    /**
     * This can be used as initialization method AFTER view gets attached (so view is available)
     */
    open fun onViewAttached() { }

    /**
     * This can be used as cleanup BEFORE unattaching a view
     */
    open fun onViewUnattaching() { }

    /**
     * This can be used to do cleanup when the view is getting destroyed
     * Base Presenter corouting scope gets cancelled right before this method is called
     */
    open fun onDestroying() { }

    open fun onStart() {
        log.i { "Lifecycle: START" }
        this.dependants?.forEach { it.onStart() }
    }
    open fun onResume() {
        log.i { "Lifecycle: RESUME" }
        this.dependants?.forEach { it.onResume() }
    }
    open fun onPause() {
        log.i { "Lifecycle: PAUSE" }
        this.dependants?.forEach { it.onPause() }
    }
    open fun onStop() {
        log.i { "Lifecycle: STOP" }
        this.dependants?.forEach { it.onStop() }
    }

    fun onDestroy() {
        log.i { "Lifecycle: DESTROY" }
        dependants?.forEach { it.onDestroy() }
        rootPresenter?.unregisterChild(this)
        presenterScope.cancel()
        onDestroying()
    }

    fun attachView(view: Any) {
        // at the moment the attach view is with the activity/ main view in ios
        // unless we change this there is no point in sharing with dependents
        this.view = view
        log.i { "Lifecycle: View Attached from Presenter" }
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
            throw IllegalStateException("You can't unregister to a non root presenter")
        }
        this.dependants!!.remove(child)
    }

    private fun isRoot() = rootPresenter == null
}