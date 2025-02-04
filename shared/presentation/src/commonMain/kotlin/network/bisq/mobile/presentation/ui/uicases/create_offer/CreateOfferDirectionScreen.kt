package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
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
fun CreateOfferBuySellScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val presenter: CreateOfferDirectionPresenter = koinInject()
    presenter.appStrings = LocalStrings.current // TODO find a more elegant solution
    RememberPresenterLifecycle(presenter)

    val showSellerReputationWarning = presenter.showSellerReputationWarning.collectAsState().value

    MultiScreenWizardScaffold(
        strings.bisqEasy_tradeWizard_review_nextButton_createOffer,
        stepIndex = 1,
        stepsLength = 6,
        horizontalAlignment = Alignment.Start,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() }
    ) {
        BisqText.h3Regular(presenter.headline)

        BisqGap.V2()

        val buyBackgroundColor by animateColorAsState(
            targetValue = if (presenter.direction == DirectionEnum.BUY) BisqTheme.colors.primary else BisqTheme.colors.dark5
        )
        BisqButton(
            onClick = { presenter.onBuySelected() },
            backgroundColor = buyBackgroundColor,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = BisqUIConstants.ScreenPadding4X),
            textComponent = { BisqText.h3Medium(text = strings.bisqEasy_tradeWizard_directionAndMarket_buy) }
        )
        BisqText.largeLight(strings.bisqEasy_tradeWizard_buy_description, color = BisqTheme.colors.grey2)

        BisqGap.V2()

        val sellBackgroundColor by animateColorAsState(
            targetValue = if (presenter.direction == DirectionEnum.SELL) BisqTheme.colors.primary else BisqTheme.colors.dark5
        )
        BisqButton(
            onClick = { presenter.onSellSelected() },
            backgroundColor = sellBackgroundColor, //BisqTheme.colors.secondary,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = BisqUIConstants.ScreenPadding4X),
            textComponent = { BisqText.h3Medium(text = strings.bisqEasy_tradeWizard_directionAndMarket_sell) }
        )
        BisqText.largeLight(strings.bisqEasy_tradeWizard_sell_description, color = BisqTheme.colors.grey2)

        if (showSellerReputationWarning) {
                SellerReputationWarningDialog(
                    onConfirm = { presenter.onSellWithoutReputation() },
                    onDismiss = { presenter.setShowSellerReputationWarning(false) },
                    onLearnReputation = { presenter.showLearnReputation() },
                )
            }
    }
}