package network.bisq.mobile.presentation.guide.wallet_guide

import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute

class WalletGuideIntroPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

    fun prevClick() {
        navigateBack()
    }

    fun introNextClick() {
        navigateTo(NavRoute.WalletGuideDownload)
    }

}
