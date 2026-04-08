package network.bisq.mobile.node.common.domain.service.alert

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData

/**
 * Stub implementation for the node app. Trade-restricting alerts are not yet
 * supported on the node — the real implementation requires AuthorizedAlertDataUtils
 * from a newer bisq2 release. This ensures the node compiles and runs without
 * the feature until proper support is added.
 */
class NodeTradeRestrictingAlertServiceFacade : TradeRestrictingAlertServiceFacade() {
    private val _alert = MutableStateFlow<AuthorizedAlertData?>(null)
    override val alert: StateFlow<AuthorizedAlertData?> = _alert.asStateFlow()
}
