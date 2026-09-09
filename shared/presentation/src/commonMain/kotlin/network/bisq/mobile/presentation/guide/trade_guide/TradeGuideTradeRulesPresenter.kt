package network.bisq.mobile.presentation.guide.trade_guide

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.guide.GuideUiAction
import network.bisq.mobile.presentation.main.MainPresenter

class TradeGuideTradeRulesPresenter(
    mainPresenter: MainPresenter,
    private val settingsServiceFacade: SettingsServiceFacade,
) : BasePresenter(mainPresenter) {
    val uiState: StateFlow<TradeGuideTradeRulesUiState> =
        settingsServiceFacade.tradeRulesConfirmed
            .map { TradeGuideTradeRulesUiState(tradeRulesConfirmed = it) }
            .stateIn(
                presenterScope,
                SharingStarted.Eagerly,
                TradeGuideTradeRulesUiState(tradeRulesConfirmed = settingsServiceFacade.tradeRulesConfirmed.value),
            )

    private val _isTradeRulesNextEnabled = MutableStateFlow(true)
    val isTradeRulesNextEnabled: StateFlow<Boolean> = _isTradeRulesNextEnabled.asStateFlow()

    fun onAction(action: GuideUiAction) {
        when (action) {
            GuideUiAction.OnPrevClick -> navigateBack()
            GuideUiAction.OnNextClick -> onTradeRulesNext()
        }
    }

    private fun onTradeRulesNext() {
        guardedSuspendAction(
            _isTradeRulesNextEnabled,
            "tradeRulesNextClick",
            reEnableGuardOnComplete = false,
        ) {
            try {
                val isConfirmed = settingsServiceFacade.tradeRulesConfirmed.first()
                if (!isConfirmed) {
                    settingsServiceFacade.confirmTradeRules(true)
                }
                navigateBackTo(NavRoute.TradeGuideSecurity, true, false)
                navigateBack()
            } catch (e: Exception) {
                _isTradeRulesNextEnabled.value = true
                throw e
            }
        }
    }
}
