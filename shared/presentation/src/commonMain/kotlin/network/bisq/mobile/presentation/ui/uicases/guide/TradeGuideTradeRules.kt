package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.list.OrderedList
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun TradeGuideTradeRules() {
    val presenter: TradeGuideTradeRulesPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val userAgreed by presenter.tradeRulesConfirmed.collectAsState()
    var localUserAgreed by remember(userAgreed) { mutableStateOf(userAgreed) }
    val isInteractive by presenter.isInteractive.collectAsState()

    val title = "bisqEasy.tradeGuide.tabs.headline".i18n() + ": " + "bisqEasy.tradeGuide.rules".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 4,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::tradeRulesNextClick,
        nextButtonText = "action.finish".i18n(),
        nextDisabled = !localUserAgreed,
        horizontalAlignment = Alignment.Start,
        isInteractive = isInteractive,
        showJumpToBottom = true
    ) {
        BisqText.h3Light("bisqEasy.tradeGuide.rules.headline".i18n())

        BisqGap.V2()

        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero)) {
            OrderedList("1.", "mobile.tradeGuide.rules.rules1".i18n())
            OrderedList("2.", "mobile.tradeGuide.rules.rules2".i18n())
            OrderedList("3.", "mobile.tradeGuide.rules.rules3".i18n())
            OrderedList("4.", "mobile.tradeGuide.rules.rules4".i18n())
            OrderedList("5.", "mobile.tradeGuide.rules.rules5".i18n())
            OrderedList("6.", "mobile.tradeGuide.rules.rules6".i18n(), includeBottomPadding = false)
        }

        BisqGap.V2()

        BisqText.baseLight("mobile.tradeGuide.rules.support".i18n())

        BisqGap.V1()

        LinkButton(
            "action.learnMore".i18n(),
            link = BisqLinks.BISQ_EASY_WIKI_URL,
            onClick = { presenter.navigateSecurityLearnMore() }
        )

        BisqGap.V1()

        if (!userAgreed)
            BisqCheckbox(
                "tac.confirm".i18n(),
                checked = localUserAgreed,
                onCheckedChange = {
                    localUserAgreed = it
                }
            )

    }
}

