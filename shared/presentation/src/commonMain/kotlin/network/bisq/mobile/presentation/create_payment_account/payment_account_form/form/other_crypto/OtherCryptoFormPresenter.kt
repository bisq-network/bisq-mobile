package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.other_crypto

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccountPayload
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto.CryptoAccountFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto.CryptoAccountFormUiState
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.main.MainPresenter

open class OtherCryptoFormPresenter(
    mainPresenter: MainPresenter,
) : CryptoAccountFormPresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(OtherCryptoFormUiState())
    override val uiState: StateFlow<OtherCryptoFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<OtherCryptoFormEffect>()
    val effect = _effect.asSharedFlow()

    lateinit var paymentMethod: CryptoPaymentMethodVO

    fun initialize(paymentMethod: CryptoPaymentMethodVO) {
        this.paymentMethod = paymentMethod
    }

    override fun updateCryptoUiState(transform: (CryptoAccountFormUiState) -> CryptoAccountFormUiState) {
        _uiState.update { it.copy(crypto = transform(it.crypto)) }
    }

    override fun onNextClick() {
        if (!::paymentMethod.isInitialized) {
            return
        }

        val validatedState =
            _uiState.updateAndGet { current ->
                current.copy(
                    crypto =
                        validateCryptoAutoConfState(current.crypto).copy(
                            addressEntry = current.crypto.addressEntry.validate(),
                        ),
                )
            }

        val validatedAccountName = validateUniqueAccountNameEntry()
        val isValid =
            validatedAccountName.isValid &&
                validatedState.crypto.addressEntry.isValid &&
                isCryptoAutoConfStateValid(validatedState.crypto)

        if (!isValid) {
            return
        }

        val payload =
            OtherCryptoAssetAccountPayload(
                address =
                    validatedState.crypto.addressEntry.value
                        .trim(),
                isInstant = validatedState.crypto.isInstant,
                isAutoConf = validatedState.crypto.isAutoConf,
                autoConfNumConfirmations =
                    if (validatedState.crypto.isAutoConf) {
                        validatedState.crypto.autoConfNumConfirmationsEntry.value
                            .trim()
                            .toIntOrNull()
                    } else {
                        null
                    },
                autoConfMaxTradeAmount =
                    if (validatedState.crypto.isAutoConf) {
                        validatedState.crypto.autoConfMaxTradeAmountEntry.value
                            .trim()
                            .toLongOrNull()
                    } else {
                        null
                    },
                autoConfExplorerUrls =
                    if (validatedState.crypto.isAutoConf) {
                        validatedState.crypto.autoConfExplorerUrlsEntry.value
                            .trim()
                            .ifBlank { null }
                    } else {
                        null
                    },
                currencyCode = paymentMethod.code,
                currencyName = paymentMethod.name,
                supportAutoConf = paymentMethod.supportAutoConf,
            )

        presenterScope.launch {
            _effect.emit(
                OtherCryptoFormEffect.NavigateToNextScreen(
                    account =
                        OtherCryptoAssetAccount(
                            accountName = uniqueAccountNameEntry.value.value.trim(),
                            accountPayload = payload,
                            creationDate = null,
                            tradeLimitInfo = paymentMethod.tradeLimitInfo,
                            tradeDuration = paymentMethod.tradeDuration,
                        ),
                ),
            )
        }
    }
}
