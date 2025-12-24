package network.bisq.mobile.presentation.guide.wallet_guide

import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter

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
