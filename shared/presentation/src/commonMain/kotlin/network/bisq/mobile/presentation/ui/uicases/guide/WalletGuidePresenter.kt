package network.bisq.mobile.presentation.ui.uicases.guide

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class WalletGuidePresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

    val blueWalletLink = "https://bluewallet.io"
    val tutorial1Link = "https://www.youtube.com/watch?v=NqY3wBhloH4"
    val tutorial2Link = "https://www.youtube.com/watch?v=imMX7i4qpmg"

    fun prevClick() {
        navigateBack()
    }

    fun introNextClick() {
        navigateTo(Routes.WalletGuideDownload)
    }

    fun downloadNextClick() {
        navigateTo(Routes.WalletGuideNewWallet)
    }

    fun newWalletNextClick() {
        navigateTo(Routes.WalletGuideReceiving)
    }

    fun receivingNextClick() {
        navigateBackTo(Routes.WalletGuideIntro, true, false)
    }

    fun navigateToBlueWallet() {
        disableInteractive()
        navigateToUrl(blueWalletLink)
        enableInteractive()
    }

    fun navigateToBlueWalletTutorial1() {
        disableInteractive()
        navigateToUrl(tutorial1Link)
        enableInteractive()
    }

    fun navigateToBlueWalletTutorial2() {
        disableInteractive()
        navigateToUrl(tutorial2Link)
        enableInteractive()
    }

}
