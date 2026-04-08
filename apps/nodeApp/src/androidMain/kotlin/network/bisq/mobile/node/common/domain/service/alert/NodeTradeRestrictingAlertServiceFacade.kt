package network.bisq.mobile.node.common.domain.service.alert

import bisq.bonded_roles.release.AppType
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertDataUtils
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.node.common.domain.mapping.alert.toDomainOrNull
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData as BisqAuthorizedAlertData

class NodeTradeRestrictingAlertServiceFacade(
    private val provider: AndroidApplicationService.Provider,
) : TradeRestrictingAlertServiceFacade() {
    private val alertService by lazy { provider.alertService.get() }

    private val _alert = MutableStateFlow<AuthorizedAlertData?>(null)
    override val alert: StateFlow<AuthorizedAlertData?> = _alert.asStateFlow()

    private var unconsumedAlertsPin: Pin? = null

    override suspend fun activate() {
        super.activate()

        refreshAlert()
        unconsumedAlertsPin =
            alertService.authorizedAlertDataSet.addObserver(
                object : CollectionObserver<BisqAuthorizedAlertData> {
                    override fun onAdded(bisqAuthorizedAlertData: BisqAuthorizedAlertData) {
                        refreshAlert()
                    }

                    override fun onRemoved(element: Any) {
                        refreshAlert()
                    }

                    override fun onCleared() {
                        refreshAlert()
                    }
                },
            )
    }

    override suspend fun deactivate() {
        unconsumedAlertsPin?.unbind()
        unconsumedAlertsPin = null
        super.deactivate()
        _alert.value = null
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
