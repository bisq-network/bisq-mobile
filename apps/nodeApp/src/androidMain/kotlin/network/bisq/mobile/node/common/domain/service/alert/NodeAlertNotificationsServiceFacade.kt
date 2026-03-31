package network.bisq.mobile.node.common.domain.service.alert

import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.service.alert.AlertNotificationsServiceFacade
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.node.common.domain.mapping.alert.toDomainOrNull
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData as BisqAuthorizedAlertData

class NodeAlertNotificationsServiceFacade(
    private val provider: AndroidApplicationService.Provider,
) : AlertNotificationsServiceFacade() {
    private val alertNotificationsService by lazy { provider.alertNotificationsService.get() }

    private val _alerts = MutableStateFlow<List<AuthorizedAlertData>>(emptyList())
    override val alerts: StateFlow<List<AuthorizedAlertData>> = _alerts.asStateFlow()

    private var unconsumedAlertsPin: Pin? = null

    override suspend fun activate() {
        super.activate()

        refreshAlerts()
        unconsumedAlertsPin =
            alertNotificationsService.unconsumedAlerts.addObserver(
                object : CollectionObserver<BisqAuthorizedAlertData> {
                    override fun onAdded(bisqAuthorizedAlertData: BisqAuthorizedAlertData) {
                        refreshAlerts()
                    }

                    override fun onRemoved(element: Any) {
                        refreshAlerts()
                    }

                    override fun onCleared() {
                        refreshAlerts()
                    }
                },
            )
    }

    override suspend fun deactivate() {
        unconsumedAlertsPin?.unbind()
        unconsumedAlertsPin = null
        _alerts.value = emptyList()
        super.deactivate()
    }

    override fun dismissAlert(alertId: String) {
        val authorizedAlertData = alertNotificationsService.unconsumedAlerts.firstOrNull { it.id == alertId } ?: return
        alertNotificationsService.dismissAlert(authorizedAlertData)
        refreshAlerts()
    }

    private fun refreshAlerts() {
        val mappedAlerts =
            alertNotificationsService.unconsumedAlerts.mapNotNull { it.toDomainOrNull() }
        _alerts.value = mappedAlerts
    }
}
