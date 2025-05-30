package network.bisq.mobile.presentation.ui.uicases.guide

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TradeGuidePresenter(
    mainPresenter: MainPresenter,
    private val settingsServiceFacade: SettingsServiceFacade
) : BasePresenter(mainPresenter) {

    val tradeRulesConfirmed: StateFlow<Boolean> = settingsServiceFacade.tradeRulesConfirmed

    fun prevClick() {
        navigateBack()
    }

    fun overviewNextClick() {
        navigateTo(Routes.TradeGuideSecurity)
    }

    fun securityNextClick() {
        navigateTo(Routes.TradeGuideProcess)
    }

    fun processNextClick() {
        navigateTo(Routes.TradeGuideTradeRules)
    }

    fun tradeRulesNextClick() {
        launchUI {
            val isConfirmed = tradeRulesConfirmed.first()
            if (!isConfirmed) {
                settingsServiceFacade.confirmTradeRules(true)
            }
            navigateBackTo(Routes.TradeGuideSecurity, true, false)
            navigateBack()
        }
    }

    fun navigateSecurityLearnMore() {
        disableInteractive()
        navigateToUrl("https://bisq.wiki/Bisq_Easy")
        enableInteractive()
    }

}
