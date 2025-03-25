package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.organisms.BisqPagerView
import network.bisq.mobile.presentation.ui.composeModels.PagerViewItem
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

interface IOnboardingPresenter : ViewPresenter {

    val onBoardingData: List<PagerViewItem>

    val indexesToShow: List<Number>
    fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState)
}

@Composable
fun OnBoardingScreen() {
    val presenter: IOnboardingPresenter = koinInject()

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { presenter.indexesToShow.size })

    RememberPresenterLifecycle(presenter)

    val finalPages = presenter.onBoardingData.filterIndexed { index, _ ->
        presenter.indexesToShow.contains(index)
    }

    BisqScrollScaffold {
        BisqLogo()
        Spacer(modifier = Modifier.height(24.dp))
        BisqText.h1LightGrey("onboarding.bisq2.headline".i18n())
        Spacer(modifier = Modifier.height(24.dp))
        BisqPagerView(pagerState, finalPages)
        Spacer(modifier = Modifier.height(24.dp))

        BisqButton(
            text = if (pagerState.currentPage == presenter.indexesToShow.lastIndex)
                "Create profile" //TODO:i18n
            else
                "action.next".i18n(),
            onClick = { presenter.onNextButtonClick(coroutineScope, pagerState) }
        )
    }
}