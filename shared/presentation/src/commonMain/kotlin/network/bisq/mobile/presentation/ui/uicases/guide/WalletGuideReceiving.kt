package network.bisq.mobile.presentation.ui.uicases.guide

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun WalletGuideReceiving() {
    val presenter: WalletGuideReceivingPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    val title = "bisqEasy.walletGuide.receive".i18n() + " - " + "bisqEasy.walletGuide.tabs.headline".i18n()

    var showSecondImage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10 * 1000)
            showSecondImage = !showSecondImage
        }
    }

    MultiScreenWizardScaffold(
        title = title,
        stepIndex = 4,
        stepsLength = 4,
        prevOnClick = presenter::prevClick,
        nextOnClick = presenter::receivingNextClick,
        horizontalAlignment = Alignment.Start,
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {
        BisqText.h3Regular("bisqEasy.walletGuide.receive.headline".i18n())

        BisqGap.V2()

        BisqText.baseRegular("bisqEasy.walletGuide.receive.content".i18n())

        BisqGap.V2()

        LinkButton(
            "bisqEasy.walletGuide.receive.link1".i18n(),
            link = BisqLinks.BLUE_WALLET_TUTORIAL_1_URL,
            onClick = { presenter.navigateToBlueWalletTutorial1() }
        )

        LinkButton(
            "bisqEasy.walletGuide.receive.link2".i18n(),
            link = BisqLinks.BLUE_WALLET_TUTORIAL_2_URL,
            onClick = { presenter.navigateToBlueWalletTutorial2() }
        )

        BisqGap.V2()

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DynamicImage(
                "drawable/wallet_guide/blue_wallet_qr.png",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = showSecondImage,
                enter = fadeIn(animationSpec = tween(1000)),
                exit = fadeOut(animationSpec = tween(1000))
            ) {
                DynamicImage(
                    "drawable/wallet_guide/blue_wallet_tx.png",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }

    }
}

