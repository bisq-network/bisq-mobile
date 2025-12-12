package network.bisq.mobile.presentation.guide.trade_guide

import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute

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
