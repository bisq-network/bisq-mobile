package network.bisq.mobile.presentation.guide.trade_guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.OrderedTextList
import network.bisq.mobile.presentation.common.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
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
        nextButtonText = "mobile.action.finish".i18n(),
        nextDisabled = !localUserAgreed,
        horizontalAlignment = Alignment.Start,
        isInteractive = isInteractive,
        showJumpToBottom = true,
    ) {
        BisqText.H3Light("bisqEasy.tradeGuide.rules.headline".i18n())

        BisqGap.V2()

        OrderedTextList(
            "bisqEasy.tradeGuide.rules.content".i18n(),
            regex = "- ",
            style = { t, m ->
                BisqText.BaseLight(
                    text = t,
                    modifier = m,
                    color = BisqTheme.colors.light_grey40,
                )
            },
        )

        BisqGap.V1()

        LinkButton(
            "action.learnMore".i18n(),
            link = BisqLinks.BISQ_EASY_WIKI_URL,
            onClick = { presenter.navigateSecurityLearnMore() },
        )

        BisqGap.V1()

        if (!userAgreed) {
            BisqCheckbox(
                label = "tac.confirm".i18n(),
                checked = localUserAgreed,
                onCheckedChange = {
                    localUserAgreed = it
                },
            )
        }
    }
}
