package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a

data class BuyerState2aUiState(
    val isTradeLoaded: Boolean = false,
    val quoteAmountWithCode: String = "",
    val paymentAccountData: String? = null,
    val tradeShortId: String = "",
    val isConfirmFiatSentEnabled: Boolean = false,
    val isAccountDataBanned: Boolean = false,
    val isBannedWarningVisible: Boolean = false,
    val isBannedCancelFailed: Boolean = false,
)
