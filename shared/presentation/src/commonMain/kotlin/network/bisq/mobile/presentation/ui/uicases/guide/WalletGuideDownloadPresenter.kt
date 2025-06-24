package network.bisq.mobile.presentation.ui.uicases.guide

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class WalletGuideDownloadPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

    val blueWalletLink = "https://bluewallet.io"

    fun prevClick() {
        navigateBack()
    }

    fun downloadNextClick() {
        navigateTo(Routes.WalletGuideNewWallet)
    }

    fun navigateToBlueWallet() {
        navigateToUrl(blueWalletLink)
    }

}
