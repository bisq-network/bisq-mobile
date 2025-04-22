package network.bisq.mobile.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.utils.Logging

abstract class ServiceFacade : LifeCycleAware, Logging {
    protected var serviceScope = CoroutineScope(IODispatcher + SupervisorJob())

    override fun activate() {
        resetServiceScope()
    }

    override fun deactivate() {
        serviceScope.cancel()
    }

    protected fun resetServiceScope() {
        this.serviceScope.cancel()
        this.serviceScope = CoroutineScope(IODispatcher + SupervisorJob())
    }
}