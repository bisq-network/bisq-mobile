package network.bisq.mobile.presentation.ui.uicases.guide

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.navigation.NavRoute

class TradeGuideTradeRulesPresenter(
    mainPresenter: MainPresenter,
    private val settingsServiceFacade: SettingsServiceFacade
) : BasePresenter(mainPresenter) {

    val tradeRulesConfirmed: StateFlow<Boolean> get() = settingsServiceFacade.tradeRulesConfirmed

    fun prevClick() {
        navigateBack()
    }

    fun tradeRulesNextClick() {
        showLoading()
        presenterScope.launch {
            try {
                val isConfirmed = tradeRulesConfirmed.first()
                if (!isConfirmed) {
                    settingsServiceFacade.confirmTradeRules(true)
                }
                navigateBackTo(NavRoute.TradeGuideSecurity, true, false)
                navigateBack()
            } finally {
                hideLoading()
            }
        }
    }

    fun navigateSecurityLearnMore() {
        navigateToUrl(BisqLinks.BISQ_EASY_WIKI_URL)
    }

}
