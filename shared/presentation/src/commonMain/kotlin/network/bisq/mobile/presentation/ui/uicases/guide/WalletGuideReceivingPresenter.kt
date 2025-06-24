package network.bisq.mobile.presentation.ui.uicases.guide

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqConfig
import network.bisq.mobile.presentation.ui.navigation.Routes

class WalletGuideReceivingPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

    val tutorial1Link = "https://www.youtube.com/watch?v=NqY3wBhloH4"
    val tutorial2Link = "https://www.youtube.com/watch?v=imMX7i4qpmg"

    fun prevClick() {
        navigateBack()
    }

    fun receivingNextClick() {
        navigateBackTo(Routes.WalletGuideIntro, true, false)
    }

    fun navigateToBlueWalletTutorial1() {
        navigateToUrl(BisqConfig.BLUE_WALLET_TUTORIAL_1_URL)
    }

    fun navigateToBlueWalletTutorial2() {
        navigateToUrl(BisqConfig.BLUE_WALLET_TUTORIAL_2_URL)
    }

}
