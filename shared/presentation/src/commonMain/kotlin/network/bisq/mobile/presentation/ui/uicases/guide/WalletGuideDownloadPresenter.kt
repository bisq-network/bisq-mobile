package network.bisq.mobile.presentation.ui.uicases.guide

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.NavRoute

class WalletGuideDownloadPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

    val blueWalletLink = "https://bluewallet.io"

    fun prevClick() {
        navigateBack()
    }

    fun downloadNextClick() {
        navigateTo(NavRoute.WalletGuideNewWallet)
    }

    fun navigateToBlueWallet() {
        navigateToUrl(blueWalletLink)
    }

}
