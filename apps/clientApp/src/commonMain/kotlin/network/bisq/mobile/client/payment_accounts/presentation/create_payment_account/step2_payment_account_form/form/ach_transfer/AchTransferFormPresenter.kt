package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.ach_transfer

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.CreateAchTransferAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.CreateAchTransferAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.main.MainPresenter

open class AchTransferFormPresenter(
    mainPresenter: MainPresenter,
) : AccountFormPresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(AchTransferFormUiState())
    override val uiState: StateFlow<AchTransferFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AchTransferFormEffect>()
    val effect = _effect.asSharedFlow()

    fun onAction(action: AchTransferFormUiAction) {
        when (action) {
            is AchTransferFormUiAction.OnHolderNameChange -> updateEntry { it.copy(holderNameEntry = it.holderNameEntry.updateValue(action.value)) }
            is AchTransferFormUiAction.OnHolderAddressChange -> updateEntry { it.copy(holderAddressEntry = it.holderAddressEntry.updateValue(action.value)) }
            is AchTransferFormUiAction.OnBankNameChange -> updateEntry { it.copy(bankNameEntry = it.bankNameEntry.updateValue(action.value)) }
            is AchTransferFormUiAction.OnRoutingNrChange -> updateEntry { it.copy(routingNrEntry = it.routingNrEntry.updateValue(action.value)) }
            is AchTransferFormUiAction.OnAccountNrChange -> updateEntry { it.copy(accountNrEntry = it.accountNrEntry.updateValue(action.value)) }
            is AchTransferFormUiAction.OnBankAccountTypeSelect -> {
                _uiState.update { it.copy(selectedBankAccountType = action.type, bankAccountTypeErrorMessage = null) }
            }
        }
    }

    private fun updateEntry(update: (AchTransferFormUiState) -> AchTransferFormUiState) {
        _uiState.update(update)
    }

    override fun onNextClick() {
        val validatedState =
            _uiState.updateAndGet { current ->
                current.copy(
                    holderNameEntry = current.holderNameEntry.validate(),
                    holderAddressEntry = current.holderAddressEntry.validate(),
                    bankNameEntry = current.bankNameEntry.validate(),
                    routingNrEntry = current.routingNrEntry.validate(),
                    accountNrEntry = current.accountNrEntry.validate(),
                    bankAccountTypeErrorMessage = validateBankAccountType(current.selectedBankAccountType),
                )
            }
        val validatedAccountName = validateUniqueAccountNameEntry()
        val bankAccountType = validatedState.selectedBankAccountType

        val isValid =
            validatedState.holderNameEntry.isValid &&
                validatedState.holderAddressEntry.isValid &&
                validatedState.bankNameEntry.isValid &&
                validatedState.routingNrEntry.isValid &&
                validatedState.accountNrEntry.isValid &&
                validatedState.bankAccountTypeErrorMessage == null &&
                bankAccountType != null &&
                validatedAccountName.isValid

        if (!isValid) {
            return
        }

        presenterScope.launch {
            _effect.emit(
                AchTransferFormEffect.NavigateToNextScreen(
                    CreateAchTransferAccount(
                        accountName = validatedAccountName.value.trim(),
                        accountPayload =
                            CreateAchTransferAccountPayload(
                                holderName = validatedState.holderNameEntry.value.trim(),
                                holderAddress = validatedState.holderAddressEntry.value.trim(),
                                bankName = validatedState.bankNameEntry.value.trim(),
                                routingNr = validatedState.routingNrEntry.value.trim(),
                                accountNr = validatedState.accountNrEntry.value.trim(),
                                bankAccountType = bankAccountType,
                            ),
                    ),
                ),
            )
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

internal fun validateHolderAddress(value: String): String? =
    validateRequiredTextMinMaxLength(
        value = value,
        minLength = PaymentAccountValidation.HOLDER_ADDRESS_MIN_LENGTH,
        maxLength = PaymentAccountValidation.HOLDER_ADDRESS_MAX_LENGTH,
    )

internal fun validateBankName(value: String): String? =
    validateRequiredTextMinMaxLength(
        value = value,
        minLength = PaymentAccountValidation.BANK_NAME_MIN_LENGTH,
        maxLength = PaymentAccountValidation.BANK_NAME_MAX_LENGTH,
    )

internal fun validateRoutingNr(value: String): String? =
    validateRequiredTextMinMaxLength(
        value = value,
        minLength = PaymentAccountValidation.BANK_ID_MIN_LENGTH,
        maxLength = PaymentAccountValidation.BANK_ID_MAX_LENGTH,
    )

internal fun validateAccountNr(value: String): String? =
    validateRequiredTextMinMaxLength(
        value = value,
        minLength = PaymentAccountValidation.ACCOUNT_NR_MIN_LENGTH,
        maxLength = PaymentAccountValidation.ACCOUNT_NR_MAX_LENGTH,
    )

private fun validateRequiredTextMinMaxLength(
    value: String,
    minLength: Int,
    maxLength: Int,
): String? {
    val trimmed = value.trim()

    try {
        NetworkDataValidation.validateRequiredText(trimmed, minLength, maxLength)
    } catch (_: IllegalArgumentException) {
        return if (trimmed.isBlank()) {
            "validation.empty".i18n()
        } else {
            "validation.tooShortOrTooLong".i18n(minLength, maxLength)
        }
    }

    return null
}

private fun validateBankAccountType(selectedBankAccountType: BankAccountType?): String? =
    if (selectedBankAccountType == null) {
        "paymentAccounts.createAccount.accountData.bank.bankAccountType.error.noneSelected".i18n()
    } else {
        null
    }
