package network.bisq.mobile.presentation.tabs.my_trades

import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
sealed interface MyTradesUiAction {
    data class OnSelectTab(
        val index: Int,
    ) : MyTradesUiAction
}
