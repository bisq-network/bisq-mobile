package network.bisq.mobile.presentation.ui.uicases.take_offer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun TakeOfferTradeAmountScreen() {
    val presenter: TakeOfferAmountPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.amount".i18n(),
        stepIndex = 1,
        stepsLength = 4,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        nextDisabled = !presenter.amountValid.collectAsState().value,
    ) {
        BisqText.h3Regular("bisqEasy.takeOffer.amount.headline.buyer".i18n())
        BisqGap.V1()
        BisqText.largeLightGrey(
            // We get currency code appended but for formattedMinAmount we want to omit it in the string
            text = "bisqEasy.takeOffer.amount.description".i18n(
                presenter.formattedMinAmount,
                presenter.formattedMaxAmountWithCode
            ),
        )

        Spacer(modifier = Modifier.height(128.dp))

        BisqAmountSelector(
            quoteCurrencyCode = presenter.quoteCurrencyCode,
            formattedMinAmount = presenter.formattedMinAmountWithCode,
            formattedMaxAmount = presenter.formattedMaxAmountWithCode,
            formattedFiatAmount = presenter.formattedQuoteAmount.collectAsState().value,
            formattedBtcAmount = presenter.formattedBaseAmount.collectAsState().value,
            onSliderValueChange = { sliderValue -> presenter.onSliderValueChanged(sliderValue) },
            onTextValueChange = { textInput -> presenter.onTextValueChanged(textInput) },
            validateTextField = { presenter.validateTextField(it) },
            sliderPosition = presenter.sliderPosition.collectAsState().value,
        )
    }
}