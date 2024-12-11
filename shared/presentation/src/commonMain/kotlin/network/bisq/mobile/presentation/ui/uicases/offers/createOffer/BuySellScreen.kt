package network.bisq.mobile.presentation.ui.uicases.offers.createOffer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferBuySellScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val presenter: ICreateOfferPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        strings.bisqEasy_tradeWizard_review_nextButton_createOffer,
        stepIndex = 1,
        stepsLength = 6,
        horizontalAlignment = Alignment.Start
    ) {
        BisqText.h3Regular(strings.bisqEasy_tradeWizard_directionAndMarket_headline)

        BisqGap.V2()

        BisqButton(
            onClick = { presenter.buyBitcoinClicked() },
            backgroundColor = BisqTheme.colors.primary,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = BisqUIConstants.ScreenPadding4X),
            textComponent = { BisqText.h3Medium(text = strings.bisqEasy_tradeWizard_directionAndMarket_buy) }
        )
        BisqText.largeLight(strings.bisqEasy_tradeWizard_buy_description, color = BisqTheme.colors.grey2)

        BisqGap.V2()

        BisqButton(
            onClick = { presenter.sellBitcoinClicked() },
            backgroundColor = BisqTheme.colors.secondary,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = BisqUIConstants.ScreenPadding4X),
            textComponent = { BisqText.h3Medium(text = strings.bisqEasy_tradeWizard_directionAndMarket_sell) }
        )
        BisqText.largeLight(strings.bisqEasy_tradeWizard_sell_description, color = BisqTheme.colors.grey2)
    }
}