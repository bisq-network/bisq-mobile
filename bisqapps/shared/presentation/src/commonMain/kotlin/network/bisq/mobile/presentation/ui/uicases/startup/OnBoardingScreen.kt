package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ViewPresenter

import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.organisms.startup.BisqPagerView
import network.bisq.mobile.presentation.ui.composeModels.PagerViewItem
import network.bisq.mobile.presentation.ui.theme.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

interface IOnboardingPresenter: ViewPresenter {

    val onBoardingData: List<PagerViewItem>

    val indexesToShow: List<Number>
    fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun OnBoardingScreen() {
    val strings = LocalStrings.current
    val navController: NavHostController = koinInject(named("RootNavController"))
    val presenter: IOnboardingPresenter = koinInject { parametersOf(navController) }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { presenter.indexesToShow.size })

    LaunchedEffect(pagerState) {
        presenter.onViewAttached()
    }

    val finalPages = presenter.onBoardingData.filterIndexed { index, _ ->
        presenter.indexesToShow.contains(index)
    }

    BisqScrollScaffold() {
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