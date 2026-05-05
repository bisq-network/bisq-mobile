package network.bisq.mobile.presentation.tabs.my_trades

import androidx.compose.runtime.Immutable

@Immutable
data class MyTradesUiState(
    val selectedTab: Int = 0,
)
