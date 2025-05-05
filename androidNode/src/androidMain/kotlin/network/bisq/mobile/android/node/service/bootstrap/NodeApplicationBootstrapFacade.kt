package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.i18n.i18n

class NodeApplicationBootstrapFacade(
    private val applicationService: AndroidApplicationService.Provider
) : ApplicationBootstrapFacade() {

    // Dependencies
    private val applicationServiceState: Observable<State> by lazy { applicationService.state.get() }

    // Misc
    private var applicationServiceStatePin: Pin? = null

    override fun activate() {
        super.activate()

        applicationServiceStatePin = applicationServiceState.addObserver { state: State ->
            when (state) {
                State.INITIALIZE_APP -> {
                    setState("splash.applicationServiceState.INITIALIZE_APP".i18n())
                    setProgress(0f)
                }

                State.INITIALIZE_NETWORK -> {
                    setState("splash.applicationServiceState.INITIALIZE_NETWORK".i18n())
                    setProgress(0.5f)
                }

                // not used
                State.INITIALIZE_WALLET -> {
                }

                State.INITIALIZE_SERVICES -> {
                    setState("splash.applicationServiceState.INITIALIZE_SERVICES".i18n())
                    setProgress(0.75f)
                }

                State.APP_INITIALIZED -> {
                    setState("splash.applicationServiceState.APP_INITIALIZED".i18n())
                    setProgress(1f)
                }

                State.FAILED -> {
                    setState("splash.applicationServiceState.FAILED".i18n())
                    setProgress(0f)
                }
            }
        }
    }

    override fun deactivate() {
        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null

        super.deactivate()
    }
}