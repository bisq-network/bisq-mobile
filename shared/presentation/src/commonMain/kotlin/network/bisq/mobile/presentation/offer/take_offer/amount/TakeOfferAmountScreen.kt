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
import network.bisq.mobile.presentation.common.ui.components.molecules.amountSelector.BisqAmountSelector
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import org.koin.compose.koinInject

@Composable
fun TakeOfferTradeAmountScreen() {
    val presenter: TakeOfferAmountPresenter = koinInject()
    val takeOfferCoordinator: TakeOfferCoordinator = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()

    TakeOfferTradeAmountContent(
        uiState = uiState,
        stepsLength = takeOfferCoordinator.totalSteps,
        onAction = presenter::onAction,
    )
}

@Composable
private fun TakeOfferTradeAmountContent(
    uiState: TakeOfferAmountUiState,
    stepsLength: Int,
    onAction: (TakeOfferAmountUiAction) -> Unit,
) {
    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.amount".i18n(),
        stepIndex = 1,
        stepsLength = stepsLength,
        prevOnClick = { onAction(TakeOfferAmountUiAction.OnBack) },
        nextOnClick = { onAction(TakeOfferAmountUiAction.OnNext) },
        nextDisabled = !uiState.amountValid,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = { onAction(TakeOfferAmountUiAction.OnClose) },
    ) {
        BisqText.H3Light("bisqEasy.takeOffer.amount.headline.buyer".i18n())
        BisqGap.V1()
        BisqText.LargeLightGrey(
            // We get currency code appended but for formattedMinAmount we want to omit it in the string
            text =
                "bisqEasy.takeOffer.amount.description".i18n(
                    uiState.formattedMinAmount,
                    uiState.formattedMaxAmountWithCode,
                ),
        )

        Spacer(modifier = Modifier.height(128.dp))

        BisqAmountSelector(
            quoteCurrencyCode = uiState.quoteCurrencyCode,
            formattedMinAmount = uiState.formattedMinAmountWithCode,
            formattedMaxAmount = uiState.formattedMaxAmountWithCode,
            formattedFiatAmount = uiState.formattedQuoteAmount,
            formattedBtcAmount = uiState.formattedBaseAmount,
            sliderPosition = uiState.sliderPosition,
            onSliderValueChange = { onAction(TakeOfferAmountUiAction.OnSliderValueChange(it)) },
            onTextValueChange = { onAction(TakeOfferAmountUiAction.OnTextValueChange(it)) },
            isError = !uiState.amountValid,
            onSliderValueChangeFinish = { onAction(TakeOfferAmountUiAction.OnSliderDragFinished) },
        )
    }
}
