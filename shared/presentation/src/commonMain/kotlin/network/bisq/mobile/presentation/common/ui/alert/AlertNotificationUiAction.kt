package network.bisq.mobile.presentation.common.ui.alert

import androidx.compose.runtime.Immutable

sealed interface AlertNotificationUiAction {
    @Immutable
    data class OnDismissAlertNotification(
        val alertId: String,
    ) : AlertNotificationUiAction

    @Immutable
    data class ExpandAlertNotification(
        val alertId: String,
    ) : AlertNotificationUiAction

    data object OnUpdateNow : AlertNotificationUiAction

    data object OnCloseDialog : AlertNotificationUiAction
}
