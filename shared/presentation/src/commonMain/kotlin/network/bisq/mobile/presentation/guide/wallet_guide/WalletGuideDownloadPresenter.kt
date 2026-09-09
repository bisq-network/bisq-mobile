package network.bisq.mobile.presentation.guide.wallet_guide

import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.guide.GuideUiAction
import network.bisq.mobile.presentation.main.MainPresenter

class WalletGuideDownloadPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    fun onAction(action: GuideUiAction) {
        when (action) {
            GuideUiAction.OnPrevClick -> navigateBack()
            GuideUiAction.OnNextClick -> navigateTo(NavRoute.WalletGuideNewWallet)
        }
    }
}
