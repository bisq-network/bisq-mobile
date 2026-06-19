package network.bisq.mobile.presentation.guide.trade_guide

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter

class TradeGuideTradeRulesPresenter(
    mainPresenter: MainPresenter,
    private val settingsServiceFacade: SettingsServiceFacade,
) : BasePresenter(mainPresenter) {
    val tradeRulesConfirmed: StateFlow<Boolean> get() = settingsServiceFacade.tradeRulesConfirmed

    private val _isTradeRulesNextEnabled = MutableStateFlow(true)
    val isTradeRulesNextEnabled: StateFlow<Boolean> = _isTradeRulesNextEnabled.asStateFlow()

    fun prevClick() {
        navigateBack()
    }

    fun tradeRulesNextClick() {
        guardedSuspendAction(
            _isTradeRulesNextEnabled,
            "tradeRulesNextClick",
            reEnableGuardOnComplete = false,
        ) {
            try {
                val isConfirmed = tradeRulesConfirmed.first()
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

    fun navigateSecurityLearnMore() {
        navigateToUrl(BisqLinks.BISQ_EASY_WIKI_URL)
    }
}
