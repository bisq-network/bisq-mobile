package network.bisq.mobile.presentation.trade.trade_detail

import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
sealed interface TradeDetailsHeaderUiAction {
    data object ToggleHeader : TradeDetailsHeaderUiAction

    data object OpenInterruptionConfirmationDialog : TradeDetailsHeaderUiAction

    data object OpenMediationConfirmationDialog : TradeDetailsHeaderUiAction
}
