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
import network.bisq.mobile.presentation.ui.composeModels.OnBoardingPage
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

open class OnBoardingPresenter(
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IOnboardingPresenter {

    private val _pagerState = MutableStateFlow<PagerState?>(null)
    override val pagerState: StateFlow<PagerState?> = _pagerState

    override fun setPagerState(pagerState: PagerState) {
        _pagerState.value = pagerState
    }

    override fun onNextButtonClick(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            val state = pagerState.value
            if (state != null) {
                if (state.currentPage == onBoardingPages.lastIndex) {
                    rootNavigator.navigate(Routes.CreateProfile.name) {
                        popUpTo(Routes.Onboarding.name) { inclusive = true }
                    }
                } else {
                    state.animateScrollToPage(state.currentPage + 1)
                }
            }
        }
    }
}
