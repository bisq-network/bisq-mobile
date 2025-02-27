package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun WalletGuideIntro() {
    val presenter: WalletGuidePresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    val title = "bisqEasy.walletGuide.intro".i18n() + " - " + "bisqEasy.walletGuide.tabs.headline".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 1,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::introNextClick,
        horizontalAlignment = Alignment.Start,
    ) {
        BisqText.h3Regular("bisqEasy.walletGuide.intro.headline".i18n())

        BisqGap.V2()

        BisqText.baseRegular("bisqEasy.walletGuide.intro.content".i18n())
    }
}

