package network.bisq.mobile.domain.service.network

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging

abstract class NetworkServiceFacade : ServiceFacade(), LifeCycleAware, Logging {
    abstract val numConnections: StateFlow<Int>
    abstract val allDataReceived: StateFlow<Boolean>
}