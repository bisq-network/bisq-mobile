package network.bisq.mobile.presentation.common.ui.alert

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.data.service.alert.AlertNotificationsServiceFacade
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.alert.banner.AlertNotificationBannerUiState
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter

@Stable
class AlertNotificationBannerPresenter(
    mainPresenter: MainPresenter,
    private val alertNotificationsServiceFacade: AlertNotificationsServiceFacade,
) : BasePresenter(mainPresenter) {
    private val currentAlertDialogId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AlertNotificationBannerUiState> =
        combine(
            alertNotificationsServiceFacade.alerts,
            currentAlertDialogId,
            mainPresenter.isMainContentVisible,
        ) { alerts, dialogAlertId, isMainContentVisible ->
            val visibleAlerts = alerts.filter { it.type.isMessageAlert() }
            val currentAlert = visibleAlerts.maxWithOrNull(compareBy({ it.type.priority }, { it.date }))?.toUiState()
            val currentAlertDialog =
                dialogAlertId
                    ?.let { id -> visibleAlerts.firstOrNull { it.id == id } }
                    ?.toUiState()
            AlertNotificationBannerUiState(
                currentAlert = currentAlert,
                pendingAlertCount = if (currentAlert == null) 0 else (visibleAlerts.size - 1).coerceAtLeast(0),
                isBannerVisible = isMainContentVisible && currentAlert != null,
                currentAlertDialog = currentAlertDialog,
            )
        }.stateIn(
            presenterScope,
            SharingStarted.WhileSubscribed(5_000),
            AlertNotificationBannerUiState(),
        )

    fun onAction(action: AlertNotificationUiAction) {
        when (action) {
            is AlertNotificationUiAction.OnDismissAlertNotification -> dismissAlert(action.alertId)
            is AlertNotificationUiAction.ExpandAlertNotification -> expandAlert(action.alertId)
            AlertNotificationUiAction.OnUpdateNow -> navigateToUrl(BisqLinks.BISQ_MOBILE_RELEASES)
            AlertNotificationUiAction.OnCloseDialog -> currentAlertDialogId.value = null
        }
    }

    private fun dismissAlert(alertId: String) {
        currentAlertDialogId.value = null
        alertNotificationsServiceFacade.dismissAlert(alertId)
    }

    private fun expandAlert(alertId: String) {
        currentAlertDialogId.value = alertId
    }

    private fun AuthorizedAlertData.toUiState(): AlertNotificationUiState =
        AlertNotificationUiState(
            id = id,
            type = type,
            headline = headline ?: defaultHeadline(type),
            message = message ?: "",
            haltTrading = haltTrading,
            requiresUpdate = requireVersionForTrading,
            minVersion = minVersion.orEmpty(),
        )

    private fun defaultHeadline(type: AlertType): String =
        when (type) {
            AlertType.INFO -> "authorizedRole.securityManager.alertType.INFO".i18n()
            AlertType.WARN -> "authorizedRole.securityManager.alertType.WARN".i18n()
            AlertType.EMERGENCY -> "authorizedRole.securityManager.alertType.EMERGENCY".i18n()
            else -> ""
        }

    private val AlertType.priority: Int
        get() =
            when (this) {
                AlertType.INFO -> 0
                AlertType.WARN -> 1
                AlertType.EMERGENCY -> 2
                else -> -1
            }
}
