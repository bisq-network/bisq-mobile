package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingPresenter

class OnBoardingNodePresenter(
    mainPresenter: MainPresenter
) : OnBoardingPresenter(mainPresenter), IOnboardingPresenter {
    override val indexesToShow = listOf(1, 2)
}
