package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.pager.PagerState
import androidx.navigation.NavController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bisq_Easy
import bisqapps.shared.presentation.generated.resources.img_fiat_btc
import bisqapps.shared.presentation.generated.resources.img_learn_and_discover
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.composeModels.PagerViewItem
import network.bisq.mobile.presentation.ui.navigation.Routes


open class OnBoardingPresenter(
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IOnboardingPresenter {

    override val indexesToShow = listOf(0)

    // TODO: Ideally slide content for xClients should only be here.
    // Android node content (along with resources), should be moved to `androidNode`
    // Then remove `indexesToShow`
    override val onBoardingData = listOf(
        PagerViewItem(
            title = "Introducing Bisq Easy",
            image = Res.drawable.img_bisq_Easy,
            desc = "Getting your first Bitcoin privately has never been easier"
        ),
        PagerViewItem(
            title = "Bisp p2p in mobile",
            image = Res.drawable.img_learn_and_discover,
            desc = "All the awesomeness of Bisq desktop now in your mobile. Android only. (TODO: Show apt image)"
        ),
        PagerViewItem(
            title = "Coming soon",
            image = Res.drawable.img_fiat_btc,
            desc = "Choose how to trade: Bisq MuSig, Lightning, Submarine Swaps,..."
        )
    )

    override fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState) {
        coroutineScope.launch {
            if (pagerState.currentPage == indexesToShow.lastIndex) {
                rootNavigator.navigate(Routes.CreateProfile.name)
            } else {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }
}
