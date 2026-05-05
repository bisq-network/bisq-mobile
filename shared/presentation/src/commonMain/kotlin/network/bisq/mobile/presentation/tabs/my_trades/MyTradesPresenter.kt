package network.bisq.mobile.presentation.tabs.my_trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class MyTradesPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(MyTradesUiState())
    val uiState: StateFlow<MyTradesUiState> = _uiState.asStateFlow()

    fun setInitialTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onAction(action: MyTradesUiAction) {
        when (action) {
            is MyTradesUiAction.OnSelectTab ->
                _uiState.update { it.copy(selectedTab = action.index) }
        }
    }
}
