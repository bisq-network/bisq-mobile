package network.bisq.mobile.presentation.common.ui.alert

import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.i18n.i18n

internal fun AuthorizedAlertData.toAlertNotificationUiState(): AlertNotificationUiState =
    AlertNotificationUiState(
        id = id,
        type = type,
        headline = headline ?: type.defaultAlertHeadline(),
        message = message ?: "",
        haltTrading = haltTrading,
        requiresUpdate = requireVersionForTrading,
        minVersion = minVersion.orEmpty(),
    )

internal fun AlertType.defaultAlertHeadline(): String =
    when (this) {
        AlertType.INFO -> "authorizedRole.securityManager.alertType.INFO".i18n()
        AlertType.WARN -> "authorizedRole.securityManager.alertType.WARN".i18n()
        AlertType.EMERGENCY -> "authorizedRole.securityManager.alertType.EMERGENCY".i18n()
        else -> ""
    }
