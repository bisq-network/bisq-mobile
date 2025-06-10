package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun TradeGuideProcess() {
    val presenter: TradeGuidePresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    val title = "bisqEasy.tradeGuide.process".i18n() + " - " + "bisqEasy.tradeGuide.tabs.headline".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 3,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::processNextClick,
        horizontalAlignment = Alignment.Start,
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {
        BisqText.h3Regular("bisqEasy.tradeGuide.process.headline".i18n())

        BisqGap.V2()

        BisqText.baseRegular("bisqEasy.tradeGuide.process.content".i18n())

        BisqGap.V1()

        BisqText.baseRegular("bisqEasy.tradeGuide.process.steps".i18n())

        BisqGap.V2()

        LinkButton(
            "action.learnMore".i18n(),
            link = "https://bisq.wiki/Bisq_Easy",
            onClick = { presenter.navigateSecurityLearnMore() }
        )
    }
}

