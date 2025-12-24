package network.bisq.mobile.presentation.offer.take_offer.amount

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferPresenter
import org.koin.compose.koinInject

@Composable
fun TakeOfferTradeAmountScreen() {
    val presenter: TakeOfferAmountPresenter = koinInject()
    val takeOfferPresenter: TakeOfferPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val formattedQuoteAmount by presenter.formattedQuoteAmount.collectAsState()
    val formattedBaseAmount by presenter.formattedBaseAmount.collectAsState()
    val sliderPosition by presenter.sliderPosition.collectAsState()
    val amountValid by presenter.amountValid.collectAsState()

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.amount".i18n(),
        stepIndex = 1,
        stepsLength = takeOfferPresenter.totalSteps,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        nextDisabled = !amountValid,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose,
    ) {
        BisqText.H3Light("bisqEasy.takeOffer.amount.headline.buyer".i18n())
        BisqGap.V1()
        BisqText.LargeLightGrey(
            // We get currency code appended but for formattedMinAmount we want to omit it in the string
            text =
                "bisqEasy.takeOffer.amount.description".i18n(
                    presenter.formattedMinAmount,
                    presenter.formattedMaxAmountWithCode,
                ),
        )

        Spacer(modifier = Modifier.height(128.dp))

        BisqAmountSelector(
            quoteCurrencyCode = presenter.quoteCurrencyCode,
            formattedMinAmount = presenter.formattedMinAmountWithCode,
            formattedMaxAmount = presenter.formattedMaxAmountWithCode,
            formattedFiatAmount = formattedQuoteAmount,
            formattedBtcAmount = formattedBaseAmount,
            onSliderValueChange = presenter::onSliderValueChanged,
            onTextValueChange = presenter::onTextValueChanged,
            validateTextField = presenter::validateTextField,
            sliderPosition = sliderPosition,
            onSliderValueChangeFinish = presenter::onSliderDragFinished,
        )
    }
}
