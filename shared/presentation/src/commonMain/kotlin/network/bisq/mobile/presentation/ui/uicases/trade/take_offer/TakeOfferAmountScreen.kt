package network.bisq.mobile.presentation.ui.uicases.trade.take_offer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun TakeOfferTradeAmountScreen() {
    val strings = LocalStrings.current.bisqEasy
    val presenter: TakeOfferAmountPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        strings.bisqEasy_takeOffer_progress_amount,
        stepIndex = 1,
        stepsLength = 3,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        useStaticScaffold = true
    ) {
        BisqText.h3Regular(
            text = strings.bisqEasy_takeOffer_amount_headline_buyer,
            color = BisqTheme.colors.light1
        )
        BisqGap.V1()
        BisqText.largeLight(
            // We get currency code appended but for formattedMinAmount we want to omit it in the string
            text = strings.bisqEasy_takeOffer_amount_description(
                presenter.formattedMinAmount,
                presenter.formattedMaxAmountWithCode
            ),
            color = BisqTheme.colors.grey2
        )

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            BisqAmountSelector(
                presenter.quoteCurrencyCode,
                presenter.formattedMinAmountWithCode,
                presenter.formattedMaxAmountWithCode,
                presenter.sliderPosition,
                presenter.formattedQuoteAmount,
                presenter.formattedBaseAmount,
                { sliderValue -> presenter.onSliderValueChanged(sliderValue) },
                { textInput -> presenter.onTextValueChanged(textInput) }
            )
        }
    }
}