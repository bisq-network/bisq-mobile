package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun WalletGuideNewWallet() {
    val presenter: WalletGuideNewPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val title = "bisqEasy.walletGuide.tabs.headline".i18n() + ": " + "bisqEasy.walletGuide.createWallet".i18n()

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 3,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::newWalletNextClick,
        horizontalAlignment = Alignment.Start,
    ) {
        BisqText.h3Light("bisqEasy.walletGuide.createWallet.headline".i18n())

        BisqGap.V2()

        BisqText.baseLight("bisqEasy.walletGuide.createWallet.content".i18n())

        BisqGap.V2()

        DynamicImage(
            "drawable/wallet_guide/blue_wallet_create.png",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

