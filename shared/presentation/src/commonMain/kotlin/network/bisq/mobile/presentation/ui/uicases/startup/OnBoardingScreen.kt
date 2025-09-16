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
    val headline: String

    val buttonText: StateFlow<String>

    val filteredPages: List<PagerViewItem>

    val indexesToShow: List<Int>

    fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState)
}

@Composable
fun OnBoardingScreen() {
    val presenter: IOnboardingPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { presenter.filteredPages.size })
    val buttonText by presenter.buttonText.collectAsState()

    BisqScrollScaffold {
        BisqGap.VHalf()
        BisqLogo()
        BisqGap.V3()
        BisqText.h2Light(presenter.headline, textAlign = TextAlign.Center)
        BisqGap.V2()
        BisqPagerView(pagerState, presenter.filteredPages)
        BisqGap.V2()

        BisqButton(
            text = if (pagerState.currentPage == presenter.indexesToShow.lastIndex)
                buttonText
            else
                "action.next".i18n(),
            onClick = { presenter.onNextButtonClick(coroutineScope, pagerState) }
        )
    }
}