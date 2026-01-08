package network.bisq.mobile.presentation.guide.trade_guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.PreviewEnvironment
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun TradeGuideOverview() {
    val presenter: TradeGuideOverviewPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()

    TradeGuideOverviewContent(
        isInteractive = isInteractive,
        prevClick = presenter::prevClick,
        nextClick = presenter::overviewNextClick,
    )
}

@Composable fun TradeGuideOverviewContent(
    isInteractive: Boolean,
    prevClick: () -> Unit,
    nextClick: () -> Unit,
) {
    val title = "bisqEasy.tradeGuide.tabs.headline".i18n() + ": " + "bisqEasy.tradeGuide.welcome".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 1,
        stepsLength = 4,
        prevOnClick = prevClick,
        nextOnClick = nextClick,
        horizontalAlignment = Alignment.Start,
        isInteractive = isInteractive,
    ) {
        BisqText.H3Light("bisqEasy.tradeGuide.welcome.headline".i18n())

        BisqGap.V2()

        BisqText.BaseLight("bisqEasy.tradeGuide.welcome.content".i18n())
    }
}

@Composable
private fun TradeGuideOverviewContentPreview(
    language: String = "en",
) {
    BisqTheme.Preview(language = language) {
        TradeGuideOverviewContent(
            isInteractive = true,
            prevClick = {},
            nextClick = {},
        )
    }
}

@Preview
@Composable
private fun TradeGuideOverviewContent_EnPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            TradeGuideOverviewContentPreview()
        }
    }
}

@Preview
@Composable
private fun TradeGuideOverviewContent_RuPreview() = TradeGuideOverviewContentPreview(language = "ru")
