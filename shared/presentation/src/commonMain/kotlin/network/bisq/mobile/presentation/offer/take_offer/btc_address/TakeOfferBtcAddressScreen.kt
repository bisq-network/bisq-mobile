package network.bisq.mobile.presentation.offer.take_offer.btc_address

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.BarcodeScannerView
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressField
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.presentation.common.ui.components.organisms.dialogs.BisqGeneralErrorDialog
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import org.koin.compose.koinInject

/**
 * Optional payout-address step for first-time buyers on mainchain trades, so they can prepare
 * (or realize they need) a wallet before committing — the funnel's biggest drop is buyers who
 * first learn mid-trade that an address is required.
 *
 * The wizard's Next button doubles as Skip: it reads "Skip for now" while the field is blank
 * (always enabled, advances without committing) and "Continue" once it has content, disabled
 * only while that content is invalid — the field's inline error already explains why.
 */
@Composable
fun TakeOfferBtcAddressScreen() {
    val presenter: TakeOfferBtcAddressPresenter = koinInject()
    val takeOfferCoordinator: TakeOfferCoordinator = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()

    val takeOffer = takeOfferCoordinator.takeOfferModel
    var stepIndex = 1
    if (takeOffer.hasAmountRange) {
        stepIndex++
    }
    if (takeOffer.hasMultipleQuoteSidePaymentMethods) {
        stepIndex++
    }
    if (takeOffer.hasMultipleBaseSidePaymentMethods) {
        stepIndex++
    }

    val nextLabel =
        if (uiState.address.isBlank()) {
            "mobile.takeOffer.addressStep.next.skip".i18n()
        } else {
            "mobile.takeOffer.addressStep.next.continue".i18n()
        }

    MultiScreenWizardScaffold(
        "mobile.takeOffer.addressStep.progress.title".i18n(),
        stepIndex = stepIndex,
        stepsLength = takeOfferCoordinator.totalSteps,
        nextButtonText = nextLabel,
        nextDisabled = uiState.address.isNotBlank() && !uiState.isValid,
        prevOnClick = { presenter.onAction(TakeOfferBtcAddressUiAction.OnBack) },
        nextOnClick = { presenter.onAction(TakeOfferBtcAddressUiAction.OnNext) },
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = { presenter.onAction(TakeOfferBtcAddressUiAction.OnClose) },
    ) {
        BisqGap.V1()
        BisqText.H3Light("mobile.takeOffer.addressStep.headline".i18n())

        BisqGap.V1()
        BisqText.BaseLight("mobile.takeOffer.addressStep.body".i18n())

        BisqGap.V2()
        BitcoinLnAddressField(
            label = "mobile.takeOffer.addressStep.fieldLabel".i18n(),
            value = uiState.address,
            onValueChange = { value, isValid ->
                presenter.onAction(TakeOfferBtcAddressUiAction.OnAddressInput(value, isValid))
            },
            type = BitcoinLnAddressFieldType.Bitcoin,
            onBarcodeClick = { presenter.onAction(TakeOfferBtcAddressUiAction.OnBarcodeClick) },
            triggerValidation = uiState.validationTrigger,
        )

        BisqGap.V2()
        BisqButton(
            text = "bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton".i18n(),
            onClick = { presenter.onAction(TakeOfferBtcAddressUiAction.OnOpenWalletGuide) },
            type = BisqButtonType.Outline,
            fullWidth = true,
        )
    }

    if (uiState.showBarcodeView) {
        BarcodeScannerView(
            onCancel = { presenter.onAction(TakeOfferBtcAddressUiAction.OnBarcodeDismiss) },
            onFail = { presenter.onAction(TakeOfferBtcAddressUiAction.OnBarcodeFail) },
        ) {
            presenter.onAction(TakeOfferBtcAddressUiAction.OnBarcodeResult(it.data))
        }
    }

    if (uiState.showBarcodeError) {
        BisqGeneralErrorDialog(
            errorTitle = "mobile.barcode.error.title".i18n(),
            errorMessage = "mobile.barcode.error.message".i18n(),
            onClose = { presenter.onAction(TakeOfferBtcAddressUiAction.OnBarcodeErrorClose) },
        )
    }
}
