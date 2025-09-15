package network.bisq.mobile.domain.service.network

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.utils.Logging

interface NetworkServiceFacade : LifeCycleAware, Logging {
    val numConnections: StateFlow<Int>
}