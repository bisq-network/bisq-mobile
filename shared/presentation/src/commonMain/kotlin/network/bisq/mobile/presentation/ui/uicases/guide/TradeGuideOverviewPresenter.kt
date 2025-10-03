package network.bisq.mobile.presentation.ui.uicases.guide

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.NavRoute

class TradeGuideOverviewPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

    fun prevClick() {
        navigateBack()
    }

    fun overviewNextClick() {
        navigateTo(NavRoute.TradeGuideSecurity)
    }

}
