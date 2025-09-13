package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.list.OrderedList
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun TradeGuideSecurity() {
    val presenter: TradeGuideSecurityPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val title = "bisqEasy.tradeGuide.tabs.headline".i18n() + ": " + "bisqEasy.tradeGuide.security".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 2,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::securityNextClick,
        horizontalAlignment = Alignment.Start,
        isInteractive = isInteractive,
    ) {
        BisqText.h3Light("bisqEasy.tradeGuide.security.headline".i18n())

        BisqGap.V2()

        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero)) {
            OrderedList("1.", "mobile.tradeGuide.security.rules1".i18n())
            OrderedList("2.", "mobile.tradeGuide.security.rules2".i18n())
            OrderedList("3.", "mobile.tradeGuide.security.rules3".i18n())
            OrderedList("4.", "mobile.tradeGuide.security.rules4".i18n(), includeBottomPadding = false)
        }

        BisqGap.V1()

        LinkButton(
            "action.learnMore".i18n(),
            link = BisqLinks.BISQ_EASY_WIKI_URL,
            onClick = { presenter.navigateSecurityLearnMore() }
        )
    }
}

