package network.bisq.mobile

import co.touchlab.kermit.Logger

/**
 * Presenter for any type of view.
 * The view should define its own interface that the child presenter should implement as well, but
 * this class provide generic useful and common behaviour for presenters
 */
abstract class BasePresenter {
    protected var view: Any? = null
    private val log = Logger.withTag("BasePresenter")

    open fun attachView(view: Any) {
        this.view = view
        log.i { "Lifecycle: View Attached from Presenter" }
    }

    open fun detachView() {
        this.view = null
        log.i { "Lifecycle: View Dettached from Presenter" }
    }

    open fun onStart() {
        log.i { "Lifecycle: START" }
    }
    open fun onResume() {
        log.i { "Lifecycle: RESUME" }
    }
    open fun onPause() {
        log.i { "Lifecycle: PAUSE" }
    }
    open fun onStop() {
        log.i { "Lifecycle: STOP" }
    }
    open fun onDestroy() {
        log.i { "Lifecycle: DESTROY" }
    }
}