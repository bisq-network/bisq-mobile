package network.bisq.mobile.data.service.alert

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData

abstract class TradeRestrictingAlertServiceFacade : ServiceFacade() {
    abstract val alert: StateFlow<AuthorizedAlertData?>
}
