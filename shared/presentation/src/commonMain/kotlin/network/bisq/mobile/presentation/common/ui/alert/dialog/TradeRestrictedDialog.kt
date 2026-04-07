package network.bisq.mobile.presentation.common.ui.alert.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiState

@Composable
internal fun TradeRestrictedDialog(
    alert: AlertNotificationUiState?,
    onAction: (AlertNotificationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (alert == null) return
    AlertNotificationDialogContent(
        alert = alert,
        onAction = onAction,
        showDismissButton = false,
        modifier = modifier,
    )
}
