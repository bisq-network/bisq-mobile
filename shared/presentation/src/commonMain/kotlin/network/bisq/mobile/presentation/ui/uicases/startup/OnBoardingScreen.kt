package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.organisms.BisqPagerView
import network.bisq.mobile.presentation.ui.composeModels.PagerViewItem
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

interface IOnboardingPresenter : ViewPresenter {

    val buttonText: StateFlow<String>

    val onBoardingData: List<PagerViewItem>

    val indexesToShow: List<Number>
    fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState)
}

@Composable
fun OnBoardingScreen() {
    val presenter: IOnboardingPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { presenter.indexesToShow.size })
    val mainButtonText by presenter.buttonText.collectAsState()

    val finalPages = presenter.onBoardingData.filterIndexed { index, _ ->
        presenter.indexesToShow.contains(index)
    }

    BisqScrollScaffold {
        BisqLogo()
        BisqGap.V2()
        BisqText.h1LightGrey("onboarding.bisq2.headline".i18n(), textAlign = TextAlign.Center)
        BisqGap.V2()
        BisqPagerView(pagerState, finalPages)
        BisqGap.V2()

        BisqButton(
            text = if (pagerState.currentPage == presenter.indexesToShow.lastIndex)
                mainButtonText
            else
                "action.next".i18n(),
            onClick = { presenter.onNextButtonClick(coroutineScope, pagerState) }
        )
    }
}