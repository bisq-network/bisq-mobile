package network.bisq.mobile.presentation.guide.wallet_guide

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.common.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun WalletGuideDownload() {
    val presenter: WalletGuideDownloadPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val title = "bisqEasy.walletGuide.tabs.headline".i18n() + ": " + "bisqEasy.walletGuide.download".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 2,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::downloadNextClick,
        horizontalAlignment = Alignment.Start,
    ) {
        BisqText.H3Light("bisqEasy.walletGuide.download.headline".i18n())

        BisqGap.V2()

        BisqText.BaseLight("bisqEasy.walletGuide.download.content".i18n())

        BisqGap.V2()

        LinkButton(
            "bisqEasy.walletGuide.download.link".i18n(),
            link = presenter.blueWalletLink,
        )

        BisqGap.V2()

        DynamicImage(
            "files/wallet_guide/blue_wallet_download.png",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
