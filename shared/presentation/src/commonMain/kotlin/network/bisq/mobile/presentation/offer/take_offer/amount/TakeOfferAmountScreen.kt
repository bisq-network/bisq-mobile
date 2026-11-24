package network.bisq.mobile.presentation.offer.take_offer.amount

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.amountSelector.BisqAmountSelector
import network.bisq.mobile.presentation.common.ui.components.organisms.create_offer.ReputationBasedBuyerLimitsPopup
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
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

        uiState.amountLimitInfo?.let { amountLimitInfo ->
            BisqGap.V2()
            NoteText(
                notes = amountLimitInfo,
                linkText = "bisqEasy.takeOffer.amount.buyer.limitInfo.learnMore".i18n(),
                onLinkClick = { onAction(TakeOfferAmountUiAction.OnShowLimitInfo) },
            )
        }
    }

    if (uiState.showLimitInfoPopup) {
        ReputationBasedBuyerLimitsPopup(
            onDismiss = { onAction(TakeOfferAmountUiAction.OnDismissLimitInfo) },
            amountLimitInfoOverlayInfo = uiState.amountLimitInfoOverlayInfo,
        )
    }
}

private val previewUiState =
    TakeOfferAmountUiState(
        quoteCurrencyCode = "USD",
        formattedMinAmount = "10.00",
        formattedMinAmountWithCode = "10.00 USD",
        formattedMaxAmountWithCode = "100.00 USD",
        formattedQuoteAmount = "55.00",
        formattedBaseAmount = "0.00055000",
        amountValid = true,
    )

@Preview
@Composable
private fun TakeOfferTradeAmountContent_Preview() {
    BisqTheme.Preview {
        TakeOfferTradeAmountContent(uiState = previewUiState, stepsLength = 3, onAction = {})
    }
}

@Preview
@Composable
private fun TakeOfferTradeAmountContent_LimitedBySellersReputation_Preview() {
    BisqTheme.Preview {
        TakeOfferTradeAmountContent(
            uiState =
                previewUiState.copy(
                    formattedMaxAmountWithCode = "30.00 USD",
                    formattedQuoteAmount = "20.00",
                    formattedBaseAmount = "0.00020000",
                    amountLimitInfo = "The seller's reputation score of 6000 allows to trade a maximum of 30.00 USD.",
                ),
            stepsLength = 3,
            onAction = {},
        )
    }
}
