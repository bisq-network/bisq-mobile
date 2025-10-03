package network.bisq.mobile.presentation.ui.uicases.guide

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.navigation.NavRoute

class TradeGuideProcessPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

    fun prevClick() {
        navigateBack()
    }

    fun processNextClick() {
        navigateTo(NavRoute.TradeGuideTradeRules)
    }

    fun navigateSecurityLearnMore() {
        navigateToUrl(BisqLinks.BISQ_EASY_WIKI_URL)
    }

}
