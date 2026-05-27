package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.cash_deposit

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.CashDepositFormUiAction
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.create.fiat.CreateCashDepositAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateCashDepositAccountPayload
import network.bisq.mobile.domain.model.account.fiat.BankAccountCountryDetails
import network.bisq.mobile.domain.model.account.fiat.BankAccountType
import network.bisq.mobile.domain.model.account.fiat.Country
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.main.MainPresenter

open class CashDepositFormPresenter(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : AccountFormPresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(CashDepositFormUiState())
    override val uiState: StateFlow<CashDepositFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<CashDepositFormEffect>()
    val effect = _effect.asSharedFlow()

    private var loadCountryDetailsJob: Job? = null

    fun initialize(paymentMethod: FiatPaymentMethod) {
        _uiState.update { current ->
            current.copy(
                countries = paymentMethod.supportedCountries.sortedBy { country -> country.name },
                currencies = paymentMethod.supportedCurrencies.sortedBy { currency -> currency.code },
            )
        }
    }

    override fun onCustomAction(action: AccountFormUiAction) {
        when (action) {
            is CashDepositFormUiAction.OnCountrySelect -> onCountrySelect(action.index)
            is CashDepositFormUiAction.OnCurrencySelect -> onCurrencySelect(action.index)
            is CashDepositFormUiAction.OnHolderNameChange -> updateEntry { it.copy(holderNameEntry = it.holderNameEntry.updateValue(action.value)) }
            is CashDepositFormUiAction.OnHolderIdChange -> updateEntry { it.copy(holderIdEntry = it.holderIdEntry.updateValue(action.value)) }
            is CashDepositFormUiAction.OnBankNameChange -> updateEntry { it.copy(bankNameEntry = it.bankNameEntry.updateValue(action.value)) }
            is CashDepositFormUiAction.OnBankIdChange -> updateEntry { it.copy(bankIdEntry = it.bankIdEntry.updateValue(action.value)) }
            is CashDepositFormUiAction.OnBranchIdChange -> updateEntry { it.copy(branchIdEntry = it.branchIdEntry.updateValue(action.value)) }
            is CashDepositFormUiAction.OnAccountNrChange -> updateEntry { it.copy(accountNrEntry = it.accountNrEntry.updateValue(action.value)) }
            is CashDepositFormUiAction.OnBankAccountTypeSelect -> {
                _uiState.update { it.copy(selectedBankAccountType = action.type, bankAccountTypeErrorMessage = null) }
            }
            is CashDepositFormUiAction.OnNationalAccountIdChange -> updateEntry { it.copy(nationalAccountIdEntry = it.nationalAccountIdEntry.updateValue(action.value)) }
            is CashDepositFormUiAction.OnRequirementsChange -> updateEntry { it.copy(requirementsEntry = it.requirementsEntry.updateValue(action.value)) }
            else -> Unit
        }
    }

    private fun onCountrySelect(index: Int) {
        val country = _uiState.value.countries.getOrNull(index) ?: return
        loadCountryDetailsJob?.cancel()
        resetCountryDependentState(index)
        loadCountryDetails(country)
    }

    private fun onCurrencySelect(index: Int) {
        _uiState.update {
            it.copy(
                selectedCurrencyIndex = index,
                currencyErrorMessage = null,
            )
        }
    }

    private fun resetCountryDependentState(selectedCountryIndex: Int) {
        _uiState.update {
            it.copy(
                selectedCountryIndex = selectedCountryIndex,
                countryErrorMessage = null,
                countryDetails = null,
                isLoadingCountryDetails = true,
                isCountryDetailsError = false,
                holderNameEntry = it.holderNameEntry.clearError(),
                holderIdEntry = it.holderIdEntry.updateValue(""),
                bankNameEntry = it.bankNameEntry.clearError(),
                bankIdEntry = it.bankIdEntry.updateValue(""),
                branchIdEntry = it.branchIdEntry.updateValue(""),
                accountNrEntry = it.accountNrEntry.clearError(),
                selectedBankAccountType = null,
                bankAccountTypeErrorMessage = null,
                nationalAccountIdEntry = it.nationalAccountIdEntry.updateValue(""),
                requirementsEntry = it.requirementsEntry.clearError(),
            )
        }
    }

    private fun loadCountryDetails(country: Country) {
        loadCountryDetailsJob =
            presenterScope.launch {
                paymentAccountsServiceFacade
                    .getBankAccountCountryDetails(country.code)
                    .onSuccess { details ->
                        _uiState.update {
                            it.copy(
                                countryDetails = details,
                                isLoadingCountryDetails = false,
                                isCountryDetailsError = false,
                            )
                        }
                    }.onFailure { error ->
                        log.e(error) { "Failed to load bank account country details for ${country.code}" }
                        _uiState.update {
                            it.copy(
                                isLoadingCountryDetails = false,
                                isCountryDetailsError = true,
                            )
                        }
                    }
            }
    }

    private fun updateEntry(update: (CashDepositFormUiState) -> CashDepositFormUiState) {
        _uiState.update(update)
    }

    override fun onNextClick() {
        var selectedCountry: Country? = null
        var selectedCurrency: FiatCurrency? = null
        val validatedState =
            _uiState.updateAndGet { current ->
                selectedCountry = current.selectedCountry
                selectedCurrency = current.selectedCurrency
                val details = current.countryDetails
                val bankAccountValidationSupported = details?.bankAccountValidationSupported == true

                current.copy(
                    countryErrorMessage = validateSelectedCountry(selectedCountry),
                    currencyErrorMessage = validateSelectedCurrency(selectedCurrency),
                    holderNameEntry = current.holderNameEntry.validate(),
                    holderIdEntry = current.holderIdEntry.validateIf(details?.holderIdRequired == true),
                    bankNameEntry = current.bankNameEntry.validateIf(bankAccountValidationSupported && details.bankNameRequired),
                    bankIdEntry = current.bankIdEntry.validateIf(bankAccountValidationSupported && details.bankIdRequired),
                    branchIdEntry = current.branchIdEntry.validateIf(bankAccountValidationSupported && details.branchIdRequired),
                    accountNrEntry = current.accountNrEntry.validate(),
                    bankAccountTypeErrorMessage = validateBankAccountType(current.selectedBankAccountType, details),
                    nationalAccountIdEntry =
                        current.nationalAccountIdEntry.validateIf(
                            bankAccountValidationSupported && details.nationalAccountIdRequired,
                        ),
                    requirementsEntry = current.requirementsEntry.validate(),
                )
            }

        val validatedAccountName = validateUniqueAccountNameEntry()
        val details = validatedState.countryDetails
        val country = selectedCountry
        val currency = selectedCurrency

        val isValid =
            country != null &&
                currency != null &&
                details != null &&
                !validatedState.isLoadingCountryDetails &&
                !validatedState.isCountryDetailsError &&
                validatedState.holderNameEntry.isValid &&
                validatedState.holderIdEntry.isValid &&
                validatedState.bankNameEntry.isValid &&
                validatedState.bankIdEntry.isValid &&
                validatedState.branchIdEntry.isValid &&
                validatedState.accountNrEntry.isValid &&
                validatedState.bankAccountTypeErrorMessage == null &&
                validatedState.nationalAccountIdEntry.isValid &&
                validatedState.requirementsEntry.isValid &&
                validatedAccountName.isValid

        if (!isValid) {
            return
        }

        presenterScope.launch {
            _effect.emit(
                CashDepositFormEffect.NavigateToNextScreen(
                    CreateCashDepositAccount(
                        accountName = validatedAccountName.value.trim(),
                        accountPayload =
                            CreateCashDepositAccountPayload(
                                selectedCountryCode = country.code,
                                selectedCurrencyCode = currency.code,
                                holderName = validatedState.holderNameEntry.value.trim(),
                                holderId = validatedState.holderIdEntry.value.trimToOptional(),
                                bankName = validatedState.bankNameEntry.value.trim(),
                                bankId = validatedState.bankIdEntry.value.trimToOptional(),
                                branchId = validatedState.branchIdEntry.value.trimToOptional(),
                                accountNr = validatedState.accountNrEntry.value.trim(),
                                bankAccountType = validatedState.selectedBankAccountType,
                                nationalAccountId = validatedState.nationalAccountIdEntry.value.trimToOptional(),
                                requirements = validatedState.requirementsEntry.value.trimToOptional(),
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

internal fun validateHolderId(value: String): String? =
    validateRequiredTextMinMaxLength(
        value = value,
        minLength = PaymentAccountValidation.HOLDER_ID_MIN_LENGTH,
        maxLength = PaymentAccountValidation.HOLDER_ID_MAX_LENGTH,
    )

internal fun validateBankName(value: String): String? =
    validateRequiredTextMinMaxLength(
        value = value,
        minLength = PaymentAccountValidation.BANK_NAME_MIN_LENGTH,
        maxLength = PaymentAccountValidation.BANK_NAME_MAX_LENGTH,
    )

internal fun validateBankId(value: String): String? =
    validateRequiredTextMinMaxLength(
        value = value,
        minLength = PaymentAccountValidation.BANK_ID_MIN_LENGTH,
        maxLength = PaymentAccountValidation.BANK_ID_MAX_LENGTH,
    )

internal fun validateBranchId(value: String): String? =
    validateRequiredTextMinMaxLength(
        value = value,
        minLength = PaymentAccountValidation.BRANCH_ID_MIN_LENGTH,
        maxLength = PaymentAccountValidation.BRANCH_ID_MAX_LENGTH,
    )

internal fun validateAccountNr(value: String): String? =
    validateRequiredTextMinMaxLength(
        value = value,
        minLength = PaymentAccountValidation.ACCOUNT_NR_MIN_LENGTH,
        maxLength = PaymentAccountValidation.ACCOUNT_NR_MAX_LENGTH,
    )

internal fun validateNationalAccountId(value: String): String? =
    validateRequiredTextMinMaxLength(
        value = value,
        minLength = PaymentAccountValidation.NATIONAL_ACCOUNT_ID_MIN_LENGTH,
        maxLength = PaymentAccountValidation.NATIONAL_ACCOUNT_ID_MAX_LENGTH,
    )

internal fun validateCashDepositRequirements(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) {
        return null
    }

    try {
        NetworkDataValidation.validateText(
            text = trimmed,
            maxLength = PaymentAccountValidation.CASH_DEPOSIT_REQUIREMENTS_MAX_LENGTH,
        )
    } catch (_: IllegalArgumentException) {
        return "validation.tooLong".i18n(PaymentAccountValidation.CASH_DEPOSIT_REQUIREMENTS_MAX_LENGTH)
    }

    return null
}

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

private fun validateSelectedCountry(country: Country?): String? =
    if (country == null) {
        "paymentAccounts.createAccount.accountData.country.error".i18n()
    } else {
        null
    }

private fun validateSelectedCurrency(currency: FiatCurrency?): String? =
    if (currency == null) {
        "paymentAccounts.createAccount.accountData.currency.error.noneSelected".i18n()
    } else {
        null
    }

private fun validateBankAccountType(
    selectedBankAccountType: BankAccountType?,
    details: BankAccountCountryDetails?,
): String? =
    if (details?.bankAccountValidationSupported == true && details.bankAccountTypeRequired && selectedBankAccountType == null) {
        "paymentAccounts.createAccount.accountData.bank.bankAccountType.error.noneSelected".i18n()
    } else {
        null
    }

private fun DataEntry.validateIf(condition: Boolean): DataEntry =
    if (condition) {
        validate()
    } else {
        clearError()
    }

private fun String.trimToOptional(): String? = trim().takeIf { it.isNotBlank() }
