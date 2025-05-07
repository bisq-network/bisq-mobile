package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun WalletGuideDownload() {
    val presenter: WalletGuidePresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    val title = "bisqEasy.walletGuide.download".i18n() + " - " + "bisqEasy.walletGuide.tabs.headline".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 2,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::downloadNextClick,
        horizontalAlignment = Alignment.Start,
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {
        BisqText.h3Regular("bisqEasy.walletGuide.download.headline".i18n())

        BisqGap.V2()

        BisqText.baseRegular("bisqEasy.walletGuide.download.content".i18n())

        BisqGap.V2()

        LinkButton(
            "bisqEasy.walletGuide.download.link".i18n(),
            link = presenter.blueWalletLink,
            onClick = { presenter.navigateToBlueWallet() }
        )

        BisqGap.V2()

        DynamicImage(
            "drawable/wallet_guide/blue_wallet_download.png",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

