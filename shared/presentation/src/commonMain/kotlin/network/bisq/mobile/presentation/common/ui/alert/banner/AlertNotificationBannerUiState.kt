package network.bisq.mobile.presentation.common.ui.alert.banner

import androidx.compose.runtime.Immutable
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiState

@Immutable
data class AlertNotificationBannerUiState(
    val currentAlert: AlertNotificationUiState? = null,
    val pendingAlertCount: Int = 0,
    val isBannerVisible: Boolean = false,
    val currentAlertDialog: AlertNotificationUiState? = null,
)
