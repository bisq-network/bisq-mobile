package network.bisq.mobile.presentation.ui.uicases.guide

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TradeGuideSecurityPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

    fun prevClick() {
        navigateBack()
    }

    fun securityNextClick() {
        navigateTo(Routes.TradeGuideProcess)
    }

    fun navigateSecurityLearnMore() {
        navigateToUrl("https://bisq.wiki/Bisq_Easy")
    }

}
