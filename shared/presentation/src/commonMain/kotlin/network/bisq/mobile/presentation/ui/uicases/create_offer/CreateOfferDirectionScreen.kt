package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.offer.SellerReputationWarningDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferDirectionScreen() {
    val presenter: CreateOfferDirectionPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val showSellerReputationWarning = presenter.showSellerReputationWarning.collectAsState().value

    MultiScreenWizardScaffold(
        "bisqEasy.tradeWizard.review.nextButton.createOffer".i18n(),
        stepIndex = 1,
        stepsLength = 6,
        horizontalAlignment = Alignment.Start,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        shouldBlurBg = showSellerReputationWarning
    ) {
        BisqText.h3Regular(presenter.headline)

        BisqGap.V2()

        val buyBackgroundColor by animateColorAsState(
            targetValue = if (presenter.direction == DirectionEnum.BUY) BisqTheme.colors.primary else BisqTheme.colors.dark_grey50
        )
        BisqButton(
            onClick = { presenter.onBuySelected() },
            backgroundColor = buyBackgroundColor,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = BisqUIConstants.ScreenPadding4X),
            textComponent = { BisqText.h3Medium("bisqEasy.tradeWizard.directionAndMarket.buy".i18n()) }
        )
        BisqGap.VHalf()
        BisqText.largeLightGrey("The easiest way to get your first Bitcoin") //TODO:i18n

        BisqGap.V2()

        val sellBackgroundColor by animateColorAsState(
            targetValue = if (presenter.direction == DirectionEnum.SELL) BisqTheme.colors.primary else BisqTheme.colors.dark_grey50
        )
        BisqButton(
            onClick = { presenter.onSellSelected() },
            backgroundColor = sellBackgroundColor, //BisqTheme.colors.secondary,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = BisqUIConstants.ScreenPadding4X),
            textComponent = { BisqText.h3Medium("bisqEasy.tradeWizard.directionAndMarket.sell".i18n()) }
        )
        BisqGap.VHalf()
        BisqText.largeLightGrey("Experienced Bisq users with reputation can act as seller") //TODO:i18n
    }

    if (showSellerReputationWarning) {
        SellerReputationWarningDialog(
            onDismiss = { presenter.onDismissSellerReputationWarning() },
            onLearnReputation = { presenter.showLearnReputation() },
        )
    }
}