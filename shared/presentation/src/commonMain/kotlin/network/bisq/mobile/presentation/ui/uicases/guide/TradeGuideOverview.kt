package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun TradeGuideOverview() {
    val presenter: TradeGuidePresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    val title = "bisqEasy.tradeGuide.welcome".i18n() + " - " + "bisqEasy.tradeGuide.tabs.headline".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 1,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::overviewNextClick,
        horizontalAlignment = Alignment.Start,
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {
        BisqText.h3Regular("bisqEasy.tradeGuide.welcome.headline".i18n())

        BisqGap.V2()

        BisqText.baseRegular("bisqEasy.tradeGuide.welcome.content".i18n())
    }
}

