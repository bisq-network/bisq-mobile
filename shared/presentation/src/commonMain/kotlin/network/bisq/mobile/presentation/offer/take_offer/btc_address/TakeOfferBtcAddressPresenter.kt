package network.bisq.mobile.presentation.offer.take_offer.btc_address

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.BitcoinAddressValidation
import network.bisq.mobile.presentation.common.ui.utils.BitcoinLightningNormalization
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.OfferFlowPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator

/**
 * The optional payout-address step of the take-offer wizard, offered to first-time buyers on
 * mainchain trades so they can prepare (or realize they need) a wallet before the trade is
 * committed. The address is only staged into the wizard model here — it reaches the peer
 * exclusively through the mid-trade screen's explicit send.
 *
 * Skip is the Next button itself: with a blank field it advances without committing anything.
 * Only a non-blank invalid value blocks Next, mirrored by the field's inline error.
 */
class TakeOfferBtcAddressPresenter(
    mainPresenter: MainPresenter,
    private val takeOfferCoordinator: TakeOfferCoordinator,
) : OfferFlowPresenter(mainPresenter) {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.TakeOfferBtcAddress

    private val _uiState =
        MutableStateFlow(
            // Re-entry (Back from review, or the review notice's Edit) restores the committed value.
            takeOfferCoordinator.takeOfferModel.btcAddress.let { committed ->
                TakeOfferBtcAddressUiState(
                    address = committed,
                    isValid = BitcoinAddressValidation.validateAddress(committed),
                )
            },
        )
    val uiState: StateFlow<TakeOfferBtcAddressUiState> = _uiState.asStateFlow()

    fun onAction(action: TakeOfferBtcAddressUiAction) {
        when (action) {
            is TakeOfferBtcAddressUiAction.OnAddressInput -> _uiState.update { it.copy(address = action.value, isValid = action.isValid) }
            TakeOfferBtcAddressUiAction.OnNext -> onNext()
            TakeOfferBtcAddressUiAction.OnBack -> {
                commitIfCoherent()
                navigateBack()
            }
            TakeOfferBtcAddressUiAction.OnClose ->
                // The X abandons the whole flow: land exactly one step below the wizard's entry,
                // same as every other step (see TakeOfferPaymentMethodPresenter.onClose).
                navigateBackTo(takeOfferCoordinator.firstScreen(), shouldInclusive = true)
            TakeOfferBtcAddressUiAction.OnOpenWalletGuide -> navigateTo(NavRoute.WalletGuideIntro)
            TakeOfferBtcAddressUiAction.OnBarcodeClick -> _uiState.update { it.copy(showBarcodeView = true) }
            TakeOfferBtcAddressUiAction.OnBarcodeDismiss -> _uiState.update { it.copy(showBarcodeView = false) }
            TakeOfferBtcAddressUiAction.OnBarcodeFail -> _uiState.update { it.copy(showBarcodeView = false, showBarcodeError = true) }
            TakeOfferBtcAddressUiAction.OnBarcodeErrorClose -> _uiState.update { it.copy(showBarcodeError = false) }
            is TakeOfferBtcAddressUiAction.OnBarcodeResult -> onBarcodeResult(action.value)
        }
    }

    private fun onNext() {
        val state = _uiState.value
        // Next is disabled on non-blank invalid input; this guard just keeps the rule honest
        // against a race between the tap and the disabling recomposition.
        if (state.address.isNotBlank() && !state.isValid) return
        commitIfCoherent()
        navigateTo(NavRoute.TakeOfferReviewTrade)
    }

    // Blank commits too: it clears a previously committed address the user has since erased.
    private fun commitIfCoherent() {
        val state = _uiState.value
        if (state.address.isBlank() || state.isValid) {
            takeOfferCoordinator.commitBtcAddress(state.address)
        }
    }

    private fun onBarcodeResult(value: String) {
        val cleaned = BitcoinLightningNormalization.cleanForValidation(value)
        // The field's validation trigger only refreshes the visual error state, so validity is
        // computed here — otherwise a scanned valid address would leave Next disabled.
        _uiState.update {
            it.copy(
                address = cleaned,
                isValid = BitcoinAddressValidation.validateAddress(cleaned),
                showBarcodeView = false,
                validationTrigger = it.validationTrigger + 1,
            )
        }
    }
}
