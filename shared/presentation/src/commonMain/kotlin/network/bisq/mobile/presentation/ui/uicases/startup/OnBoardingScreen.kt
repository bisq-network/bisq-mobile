package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.CoroutineScope
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.organisms.BisqPagerView
import network.bisq.mobile.presentation.ui.composeModels.PagerViewItem
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject

interface IOnboardingPresenter : ViewPresenter {

    val onBoardingData: List<PagerViewItem>

    val indexesToShow: List<Number>
    fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun OnBoardingScreen() {
    val strings = LocalStrings.current
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
        BisqText.h1Light(
            text = strings.onboarding_bisq2_headline,
            color = BisqTheme.colors.grey1,
        )
        Spacer(modifier = Modifier.height(56.dp))
        BisqPagerView(pagerState, finalPages)
        Spacer(modifier = Modifier.height(56.dp))

        BisqButton(
            text = if (pagerState.currentPage == presenter.indexesToShow.lastIndex) strings.onboarding_button_create_profile else strings.buttons_next,
            onClick = { presenter.onNextButtonClick(coroutineScope, pagerState) }
        )
    }
}