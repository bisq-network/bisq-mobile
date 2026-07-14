package network.bisq.mobile.node.common.domain.service.alert

import bisq.bonded_roles.release.AppType
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertDataUtils
import bisq.common.observable.Pin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.node.common.domain.mapping.alert.toDomainOrNull
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService

class NodeTradeRestrictingAlertServiceFacade(
    private val provider: AndroidApplicationService.Provider,
) : TradeRestrictingAlertServiceFacade() {
    private val alertService by lazy { provider.alertService.get() }

    private val _alert = MutableStateFlow<AuthorizedAlertData?>(null)
    override val alert: StateFlow<AuthorizedAlertData?> = _alert.asStateFlow()

    private var unconsumedAlertsPin: Pin? = null

    override suspend fun activate() {
        super.activate()

        // The Runnable observer fires once at subscription, covering the initial refresh
        unconsumedAlertsPin = alertService.authorizedAlertDataSet.addObserver(Runnable { refreshAlert() })
    }

    override suspend fun deactivate() {
        // Tear down our own state before delegating to the base (mirror of activate()).
        unconsumedAlertsPin?.unbind()
        unconsumedAlertsPin = null
        _alert.value = null
        super.deactivate()
    }

    private fun refreshAlert() {
        _alert.value =
            AuthorizedAlertDataUtils
                .findMostRecentTradeRestrictingAlert(
                    alertService.authorizedAlertDataSet.stream(),
                    AppType.MOBILE_CLIENT,
                ).map { it.toDomainOrNull() }
                .orElse(null)
    }
}
