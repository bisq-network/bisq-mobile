package network.bisq.mobile.presentation.guide.trade_guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.OrderedTextList
import network.bisq.mobile.presentation.common.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.common.ui.utils.PreviewEnvironment
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun TradeGuideSecurity() {
    val presenter: TradeGuideSecurityPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()

    TradeGuideSecurityContent(
        isInteractive = isInteractive,
        prevClick = presenter::prevClick,
        nextClick = presenter::securityNextClick,
        learnMoreClick = presenter::navigateSecurityLearnMore,
    )
}

@Composable fun TradeGuideSecurityContent(
    isInteractive: Boolean,
    prevClick: () -> Unit,
    nextClick: () -> Unit,
    learnMoreClick: () -> Unit,
) {
    val title = "bisqEasy.tradeGuide.tabs.headline".i18n() + ": " + "bisqEasy.tradeGuide.security".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 2,
        stepsLength = 4,
        prevOnClick = prevClick,
        nextOnClick = nextClick,
        horizontalAlignment = Alignment.Start,
        isInteractive = isInteractive,
    ) {
        BisqText.H3Light("bisqEasy.tradeGuide.security.headline".i18n())

        BisqGap.V2()

        OrderedTextList(
            "bisqEasy.tradeGuide.security.content".i18n(),
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
            onClick = learnMoreClick,
        )
    }
}

@Composable
private fun TradeGuideSecurityContentPreview(
    language: String = "en",
) {
    BisqTheme.Preview(language = language) {
        TradeGuideSecurityContent(
            isInteractive = true,
            prevClick = {},
            nextClick = {},
            learnMoreClick = {},
        )
    }
}

@Preview
@Composable
private fun TradeGuideSecurityContent_EnPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            TradeGuideSecurityContentPreview()
        }
    }
}

@Preview
@Composable
private fun TradeGuideSecurityContent_RuPreview() = TradeGuideSecurityContentPreview(language = "ru")
