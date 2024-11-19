package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.pager.PagerState
import androidx.navigation.NavController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bisq_Easy
import bisqapps.shared.presentation.generated.resources.img_fiat_btc
import bisqapps.shared.presentation.generated.resources.img_learn_and_discover
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.composeModels.PagerViewItem
import network.bisq.mobile.presentation.ui.navigation.Routes

val onBoardingPages = listOf(
    PagerViewItem(
        title = "Introducing Bisq Easy",
        image = Res.drawable.img_bisq_Easy,
        desc = "Getting your first Bitcoin privately has never been easier"
    ),
    PagerViewItem(
        title = "Learn & Discover",
        image = Res.drawable.img_learn_and_discover,
        desc = "Get a gentle introduction into Bitcoin through our guides and community chat"
    ),
    PagerViewItem(
        title = "Coming soon",
        image = Res.drawable.img_fiat_btc,
        desc = "Choose how to trade: Bisq MuSig, Lightning, Submarine Swaps,..."
    )
)

open class OnBoardingPresenter(
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IOnboardingPresenter {

    override fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState) {
        coroutineScope.launch {
            if (pagerState.currentPage == onBoardingPages.lastIndex) {
                 rootNavigator.navigate(Routes.CreateProfile.name)
            } else {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }
}
