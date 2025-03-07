package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun TradeGuideTradeRules() {
    val presenter: TradeGuidePresenter = koinInject()
    val userAgreed by presenter.tradeRulesConfirmed.collectAsState()
    var _userAgreed by remember { mutableStateOf(userAgreed) }

    RememberPresenterLifecycle(presenter)

    val title = "bisqEasy.tradeGuide.rules".i18n() + " - " + "bisqEasy.tradeGuide.tabs.headline".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 4,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::tradeRulesNextClick,
        nextDisabled = !_userAgreed,
        horizontalAlignment = Alignment.Start,
        isInteractive = presenter.isInteractive.collectAsState().value,
        showJumpToBottom = true
    ) {
        BisqText.h3Regular("bisqEasy.tradeGuide.rules.headline".i18n())

        BisqGap.V2()

        BisqText.baseRegular("bisqEasy.tradeGuide.rules.content".i18n())

        BisqGap.V2()

        LinkButton(
            "action.learnMore".i18n(),
            link = "https://bisq.wiki/Bisq_Easy",
            onClick = { presenter.navigateSecurityLearnMore() }
        )

        BisqGap.V1()

        if (userAgreed == false)
            BisqCheckbox(
                "tac.confirm".i18n(),
                checked = _userAgreed,
                onCheckedChange = {
                    _userAgreed = it
                }
            )

    }
}

