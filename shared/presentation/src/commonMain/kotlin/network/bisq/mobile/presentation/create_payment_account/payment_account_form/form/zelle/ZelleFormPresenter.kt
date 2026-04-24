package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.common.validation.EmailValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.data.replicated.common.validation.PhoneNumberValidation
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.ZelleFormUiAction
import network.bisq.mobile.presentation.main.MainPresenter

private const val US_REGION_CODE = "US"

open class ZelleFormPresenter(
    mainPresenter: MainPresenter,
) : AccountFormPresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(ZelleFormUiState())

    override val uiState: StateFlow<ZelleFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ZelleFormEffect>()
    val effect = _effect.asSharedFlow()

    override fun onCustomAction(action: AccountFormUiAction) {
        when (action) {
            is ZelleFormUiAction.OnHolderNameChange -> {
                _uiState.update {
                    it.copy(
                        holderNameEntry = it.holderNameEntry.updateValue(action.value),
                    )
                }
            }

            is ZelleFormUiAction.OnEmailOrMobileNrChange -> {
                _uiState.update {
                    it.copy(
                        emailOrMobileNrEntry = it.emailOrMobileNrEntry.updateValue(action.value),
                    )
                }
            }

            else -> Unit
        }
    }

    override fun onNextClick() {
        val validatedState =
            _uiState.updateAndGet {
                it.copy(
                    holderNameEntry = it.holderNameEntry.validate(),
                    emailOrMobileNrEntry = it.emailOrMobileNrEntry.validate(),
                )
            }
        val validatedAccountName = validateUniqueAccountNameEntry()

        if (validatedState.holderNameEntry.isValid && validatedState.emailOrMobileNrEntry.isValid && validatedAccountName.isValid) {
            presenterScope.launch {
                _effect.emit(
                    ZelleFormEffect.NavigateToNextScreen(
                        ZelleAccount(
                            accountName = uniqueAccountNameEntry.value.value.trim(),
                            accountPayload =
                                ZelleAccountPayload(
                                    holderName =
                                        uiState.value.holderNameEntry.value
                                            .trim(),
                                    emailOrMobileNr =
                                        uiState.value.emailOrMobileNrEntry.value
                                            .trim(),
                                ),
                        ),
                    ),
                )
            }
        }
    }
}

internal fun validateHolderName(value: String): String? {
    val trimmed = value.trim()

    try {
        PaymentAccountValidation.validateHolderName(trimmed)
    } catch (_: IllegalArgumentException) {
        return "validation.tooShortOrTooLong".i18n(
            PaymentAccountValidation.HOLDER_NAME_MIN_LENGTH,
            PaymentAccountValidation.HOLDER_NAME_MAX_LENGTH,
        )
    }

    return null
}

internal fun validateEmailOrMobile(value: String): String? {
    val trimmed = value.trim()
    val isValid =
        EmailValidation.isValid(trimmed) ||
            PhoneNumberValidation.isValid(trimmed, US_REGION_CODE)

    return if (isValid) {
        null
    } else {
        "validation.invalidEmailOrPhoneNumber".i18n()
    }
}
