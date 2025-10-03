package network.bisq.mobile.presentation.ui.uicases.guide

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.navigation.NavRoute

class WalletGuideReceivingPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

    fun prevClick() {
        navigateBack()
    }

    fun receivingNextClick() {
        navigateBackTo(NavRoute.WalletGuideIntro, true, false)
    }

    fun navigateToBlueWalletTutorial1() {
        navigateToUrl(BisqLinks.BLUE_WALLET_TUTORIAL_1_URL)
    }

    fun navigateToBlueWalletTutorial2() {
        navigateToUrl(BisqLinks.BLUE_WALLET_TUTORIAL_2_URL)
    }

}
