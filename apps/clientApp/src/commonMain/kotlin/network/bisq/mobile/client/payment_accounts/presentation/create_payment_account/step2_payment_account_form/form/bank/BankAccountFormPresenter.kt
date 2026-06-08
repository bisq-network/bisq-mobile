package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.main.MainPresenter

abstract class BankAccountFormPresenter<T : CreatePaymentAccount>(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : AccountFormPresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(BankAccountFormUiState())
    override val uiState: StateFlow<BankAccountFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<BankAccountFormEffect<T>>()
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

    fun onAction(action: BankAccountFormUiAction) {
        when (action) {
            is BankAccountFormUiAction.OnCountrySelect -> onCountrySelect(action.index)
            is BankAccountFormUiAction.OnCurrencySelect -> onCurrencySelect(action.index)
            is BankAccountFormUiAction.OnHolderNameChange -> updateEntry { it.copy(holderNameEntry = it.holderNameEntry.updateValue(action.value)) }
            is BankAccountFormUiAction.OnHolderIdChange -> updateEntry { it.copy(holderIdEntry = it.holderIdEntry.updateValue(action.value)) }
            is BankAccountFormUiAction.OnBankNameChange -> updateEntry { it.copy(bankNameEntry = it.bankNameEntry.updateValue(action.value)) }
            is BankAccountFormUiAction.OnBankIdChange -> updateEntry { it.copy(bankIdEntry = it.bankIdEntry.updateValue(action.value)) }
            is BankAccountFormUiAction.OnBranchIdChange -> updateEntry { it.copy(branchIdEntry = it.branchIdEntry.updateValue(action.value)) }
            is BankAccountFormUiAction.OnAccountNrChange -> updateEntry { it.copy(accountNrEntry = it.accountNrEntry.updateValue(action.value)) }
            is BankAccountFormUiAction.OnBankAccountTypeSelect -> {
                _uiState.update { it.copy(selectedBankAccountType = action.type, bankAccountTypeErrorMessage = null) }
            }
            is BankAccountFormUiAction.OnNationalAccountIdChange -> updateEntry { it.copy(nationalAccountIdEntry = it.nationalAccountIdEntry.updateValue(action.value)) }
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
                holderNameEntry = it.holderNameEntry.updateValue(""),
                holderIdEntry = it.holderIdEntry.updateValue(""),
                bankNameEntry = it.bankNameEntry.updateValue(""),
                bankIdEntry = it.bankIdEntry.updateValue(""),
                branchIdEntry = it.branchIdEntry.updateValue(""),
                accountNrEntry = it.accountNrEntry.clearError(),
                selectedBankAccountType = null,
                bankAccountTypeErrorMessage = null,
                nationalAccountIdEntry = it.nationalAccountIdEntry.updateValue(""),
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

    private fun updateEntry(update: (BankAccountFormUiState) -> BankAccountFormUiState) {
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
                    holderNameEntry = current.holderNameEntry.validateIf(bankAccountValidationSupported),
                    holderIdEntry = current.holderIdEntry.validateIf(bankAccountValidationSupported && details.holderIdRequired),
                    bankNameEntry = current.bankNameEntry.validateIf(bankAccountValidationSupported && details.bankNameRequired),
                    bankIdEntry = current.bankIdEntry.validateIf(bankAccountValidationSupported && details.bankIdRequired),
                    branchIdEntry = current.branchIdEntry.validateIf(bankAccountValidationSupported && details.branchIdRequired),
                    accountNrEntry = current.accountNrEntry.validate(),
                    bankAccountTypeErrorMessage = validateBankAccountType(current.selectedBankAccountType, details),
                    nationalAccountIdEntry =
                        current.nationalAccountIdEntry.validateIf(
                            bankAccountValidationSupported && details.nationalAccountIdRequired,
                        ),
                )
            }

        val validatedAccountName = validateUniqueAccountNameEntry()
        val details = validatedState.countryDetails
        val country = selectedCountry
        val currency = selectedCurrency
        val bankAccountValidationSupported = details?.bankAccountValidationSupported == true

        val isValid =
            country != null &&
                currency != null &&
                details != null &&
                !validatedState.isLoadingCountryDetails &&
                !validatedState.isCountryDetailsError &&
                validatedState.accountNrEntry.isValid &&
                (!bankAccountValidationSupported || validatedState.holderNameEntry.isValid) &&
                (!bankAccountValidationSupported || validatedState.holderIdEntry.isValid) &&
                (!bankAccountValidationSupported || validatedState.bankNameEntry.isValid) &&
                (!bankAccountValidationSupported || validatedState.bankIdEntry.isValid) &&
                (!bankAccountValidationSupported || validatedState.branchIdEntry.isValid) &&
                validatedState.bankAccountTypeErrorMessage == null &&
                (!bankAccountValidationSupported || validatedState.nationalAccountIdEntry.isValid) &&
                validatedAccountName.isValid

        if (!isValid) {
            return
        }

        presenterScope.launch {
            _effect.emit(
                BankAccountFormEffect.NavigateToNextScreen(
                    createAccount(
                        accountName = validatedAccountName.value.trim(),
                        payloadData =
                            BankAccountCreatePayloadData(
                                selectedCountryCode = country.code,
                                selectedCurrencyCode = currency.code,
                                holderName = validatedState.holderNameEntry.value.trimToOptional(),
                                holderId = validatedState.holderIdEntry.value.trimToOptional(),
                                bankName = validatedState.bankNameEntry.value.trimToOptional(),
                                bankId = validatedState.bankIdEntry.value.trimToOptional(),
                                branchId = validatedState.branchIdEntry.value.trimToOptional(),
                                accountNr = validatedState.accountNrEntry.value.trim(),
                                bankAccountType = validatedState.selectedBankAccountType,
                                nationalAccountId = validatedState.nationalAccountIdEntry.value.trimToOptional(),
                            ),
                    ),
                ),
            )
        }
    }

    protected abstract fun createAccount(
        accountName: String,
        payloadData: BankAccountCreatePayloadData,
    ): T
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
