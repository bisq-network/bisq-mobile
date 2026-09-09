package network.bisq.mobile.presentation.guide.wallet_guide

import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.guide.GuideUiAction
import network.bisq.mobile.presentation.main.MainPresenter

class WalletGuideReceivingPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    fun onAction(action: GuideUiAction) {
        when (action) {
            GuideUiAction.OnPrevClick -> navigateBack()
            // The guide's last step: leaving forward pops the whole wizard rather than stacking
            // a fifth page, so back from wherever the user came from does not replay the guide.
            GuideUiAction.OnNextClick -> navigateBackTo(NavRoute.WalletGuideIntro, true, false)
        }
    }
}
