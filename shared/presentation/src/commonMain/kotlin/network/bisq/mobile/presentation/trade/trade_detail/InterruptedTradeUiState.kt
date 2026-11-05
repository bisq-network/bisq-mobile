package network.bisq.mobile.presentation.trade.trade_detail

data class InterruptedTradeUiState(
    // Shown for cancelled and rejected trades; null hides it.
    val interruptedTradeInfo: String? = null,
    // Shown for failed trades; null hides it.
    val errorMessage: String? = null,
    val isInMediation: Boolean = false,
    val isReportToMediatorButtonVisible: Boolean = false,
    val isNoMediatorAvailableWarningVisible: Boolean = false,
    val isMediationRequestedDialogVisible: Boolean = false,
    val isCloseTradeEnabled: Boolean = true,
    val isReportToMediatorEnabled: Boolean = true,
)
