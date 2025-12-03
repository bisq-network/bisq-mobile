package network.bisq.mobile.presentation.trade.trade_detail.states.common

enum class TxConfirmationState {
    IDLE,
    REQUEST_STARTED,
    IN_MEMPOOL,
    CONFIRMED,
    FAILED
}