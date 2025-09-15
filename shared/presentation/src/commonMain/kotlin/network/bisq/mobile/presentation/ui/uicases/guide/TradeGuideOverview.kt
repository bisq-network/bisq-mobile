package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.BisqStepProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.PreviewEnvironment
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
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
        nextClick = presenter::overviewNextClick
    )
}

@Composable fun TradeGuideOverviewContent(
    isInteractive: Boolean,
    prevClick: () -> Unit,
    nextClick: () -> Unit
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
        BisqText.h3Light("bisqEasy.tradeGuide.welcome.headline".i18n())

        BisqGap.V2()

        BisqText.baseLight("bisqEasy.tradeGuide.welcome.content".i18n())
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
            nextClick = {}
        )
    }
}

@Preview
@Composable
private fun TradeGuideOverviewContentPreview_En() {
    BisqTheme.Preview {
        PreviewEnvironment {
            TradeGuideOverviewContentPreview()
        }
    }
}

@Preview
@Composable
private fun TradeGuideOverviewContentPreview_Ru() = TradeGuideOverviewContentPreview(language = "ru")
