package network.bisq.mobile.presentation.ui.uicases.guide

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TradeGuidePresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

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

    fun navigateSecurityLearnMore() {
        navigateToUrl("https://bisq.wiki/Bisq_Easy")
    }

}
