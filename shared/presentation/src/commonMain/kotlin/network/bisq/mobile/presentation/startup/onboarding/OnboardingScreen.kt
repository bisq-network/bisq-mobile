package network.bisq.mobile.presentation.startup.onboarding

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.CoroutineScope
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.ViewPresenter
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.organisms.pager_view.BisqPagerView
import network.bisq.mobile.presentation.common.ui.components.organisms.pager_view.PagerViewItem
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

interface IOnboardingPresenter : ViewPresenter {
    val headline: String

    val filteredPages: List<PagerViewItem>

    val indexesToShow: List<Int>

    fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState)
}

@Composable
fun OnboardingScreen() {
    val presenter: IOnboardingPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { presenter.filteredPages.size })

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
                "mobile.onboarding.createProfile".i18n()
            else
                "action.next".i18n(),
            onClick = { presenter.onNextButtonClick(coroutineScope, pagerState) },
            modifier = Modifier.semantics { contentDescription = "onboarding_next_button" }
        )
    }
}