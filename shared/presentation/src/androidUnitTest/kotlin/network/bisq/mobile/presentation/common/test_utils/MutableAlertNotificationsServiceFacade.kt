package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.service.alert.AlertNotificationsServiceFacade
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData

/** Builds an [AuthorizedAlertData] for alert banner/dialog UI tests. */
fun authorizedAlert(
    id: String,
    type: AlertType,
    date: Long,
    headline: String? = "Headline",
    message: String = "message",
    haltTrading: Boolean = false,
    requireVersionForTrading: Boolean = false,
    minVersion: String? = null,
): AuthorizedAlertData =
    AuthorizedAlertData(
        id = id,
        type = type,
        headline = headline,
        message = message,
        haltTrading = haltTrading,
        requireVersionForTrading = requireVersionForTrading,
        minVersion = minVersion,
        date = date,
    )

/**
 * [AlertNotificationsServiceFacade] double that exposes [alerts] and records dismissals while
 * mutating the list (UI tests assert both the recorded id and the remaining alerts).
 */
class MutableAlertNotificationsServiceFacade(
    initialAlerts: List<AuthorizedAlertData>,
) : AlertNotificationsServiceFacade() {
    private val alertsFlow = MutableStateFlow(initialAlerts)
    var lastDismissedAlertId: String? = null

    override val alerts: StateFlow<List<AuthorizedAlertData>> = alertsFlow.asStateFlow()

    override fun dismissAlert(alertId: String) {
        lastDismissedAlertId = alertId
        alertsFlow.value = alertsFlow.value.filterNot { it.id == alertId }
    }
}
