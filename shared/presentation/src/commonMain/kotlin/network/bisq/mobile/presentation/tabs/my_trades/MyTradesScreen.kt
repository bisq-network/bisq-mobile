package network.bisq.mobile.presentation.tabs.my_trades

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.common.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.tabs.my_trades.closed.ClosedTradeListScreen
import network.bisq.mobile.presentation.tabs.my_trades.open.OpenTradeListScreen
import org.koin.compose.koinInject

@Composable
fun MyTradesScreen(initialTab: Int = 0) {
    val presenter: MyTradesPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    LaunchedEffect(initialTab) { presenter.setInitialTab(initialTab) }

    val uiState by presenter.uiState.collectAsState()
    val isInteractive by presenter.isInteractive.collectAsState()
    val showHistoryTab by presenter.showHistoryTab.collectAsState()

    val tabOptions =
        buildList {
            add("mobile.myTrades.tab.open".i18n())
            if (showHistoryTab) add("mobile.myTrades.tab.history".i18n())
        }

    BisqStaticLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.Zero),
        isInteractive = isInteractive,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showHistoryTab) {
                ToggleTab(
                    options = tabOptions,
                    selectedOption = tabOptions.getOrElse(uiState.selectedTab) { tabOptions.first() },
                    onOptionSelect = { option ->
                        presenter.onAction(MyTradesUiAction.OnSelectTab(tabOptions.indexOf(option)))
                    },
                    getDisplayString = { it },
                )

                when (uiState.selectedTab) {
                    1 -> ClosedTradeListScreen()
                    else -> OpenTradeListScreen()
                }
            } else {
                OpenTradeListScreen()
            }
        }
    }
}
