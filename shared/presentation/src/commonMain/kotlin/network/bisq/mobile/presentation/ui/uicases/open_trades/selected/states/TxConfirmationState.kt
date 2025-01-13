package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

enum class TxConfirmationState {
    IDLE,
    REQUEST_STARTED,
    IN_MEMPOOL,
    CONFIRMED,
    FAILED
}