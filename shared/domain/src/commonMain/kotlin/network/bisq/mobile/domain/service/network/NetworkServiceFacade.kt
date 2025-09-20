package network.bisq.mobile.domain.service.network

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.utils.Logging

interface NetworkServiceFacade : LifeCycleAware, Logging {
    val minConnections: Int get() = 3
    val numConnections: StateFlow<Int>
    val targetNumConnectedPeers: StateFlow<Int>
    val numPendingRequests: StateFlow<Int>
    val maxPendingRequestsAtStartup: StateFlow<Int>
    val allDataReceived: StateFlow<Boolean>
}