package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun TradeGuideSecurity() {
    val presenter: TradeGuideSecurityPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val title = "bisqEasy.tradeGuide.security".i18n() + " - " + "bisqEasy.tradeGuide.tabs.headline".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 2,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::securityNextClick,
        horizontalAlignment = Alignment.Start,
        isInteractive = isInteractive,
    ) {
        BisqText.h3Regular("bisqEasy.tradeGuide.security.headline".i18n())

        BisqGap.V2()

        BisqText.baseRegular("bisqEasy.tradeGuide.security.content".i18n())

        BisqGap.V2()

        LinkButton(
            "action.learnMore".i18n(),
            link = BisqLinks.BISQ_EASY_WIKI_URL,
            onClick = { presenter.navigateSecurityLearnMore() }
        )
    }
}

