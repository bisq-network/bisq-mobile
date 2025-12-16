package network.bisq.mobile.presentation.guide.wallet_guide

import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute

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
