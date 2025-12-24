package network.bisq.mobile.presentation.guide.wallet_guide

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun WalletGuideIntro() {
    val presenter: WalletGuideIntroPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val title = "bisqEasy.walletGuide.tabs.headline".i18n() + ": " + "bisqEasy.walletGuide.intro".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 1,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::introNextClick,
        horizontalAlignment = Alignment.Start,
    ) {
        BisqText.H3Light("bisqEasy.walletGuide.intro.headline".i18n())

        BisqGap.V2()

        BisqText.BaseLight("bisqEasy.walletGuide.intro.content".i18n())
    }
}
