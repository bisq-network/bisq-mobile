package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a

data class BuyerState2aUiState(
    val isTradeLoaded: Boolean = false,
    // The trade whose data is rendered; the target of confirm and of the banned cancel.
    val tradeId: String? = null,
    val quoteAmountWithCode: String = "",
    val paymentAccountData: String? = null,
    val tradeShortId: String = "",
    val isConfirmFiatSentEnabled: Boolean = false,
    // True while the banned check has not answered yet, retries included.
    val isAccountDataCheckPending: Boolean = false,
    val isAccountDataBanned: Boolean = false,
    val isBannedWarningVisible: Boolean = false,
    val isBannedCancelFailed: Boolean = false,
)
