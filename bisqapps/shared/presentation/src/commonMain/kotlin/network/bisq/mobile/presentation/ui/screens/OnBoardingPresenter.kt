package network.bisq.mobile.presentation.ui.screens


import androidx.compose.foundation.pager.PagerState
import androidx.navigation.NavController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bisq_Easy
import bisqapps.shared.presentation.generated.resources.img_fiat_btc
import bisqapps.shared.presentation.generated.resources.img_learn_and_discover
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.ui.model.OnBoardingPage
import network.bisq.mobile.presentation.ui.navigation.Routes

val onBoardingPages = listOf(
    OnBoardingPage(
        title = "Introducing Bisq Easy",
        image = Res.drawable.img_bisq_Easy,
        desc = "Getting your first Bitcoin privately has never been easier"
    ),
    OnBoardingPage(
        title = "Learn & Discover",
        image = Res.drawable.img_learn_and_discover,
        desc = "Get a gentle introduction into Bitcoin through our guides and community chat"
    ),
    OnBoardingPage(
        title = "Coming soon",
        image = Res.drawable.img_fiat_btc,
        desc = "Choose how to trade: Bisq MuSig, Lightning, Submarine Swaps,..."
    )
)

class OnBoardingPresenter(
    private val navController: NavController,
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    // private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun onNextButtonClick() {
        coroutineScope.launch {
            if (pagerState.currentPage == onBoardingPages.lastIndex) {
                navController.navigate(Routes.CreateProfile.name) {
                    popUpTo(Routes.Onboarding.name) { inclusive = true }
                }
            } else {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }
}
