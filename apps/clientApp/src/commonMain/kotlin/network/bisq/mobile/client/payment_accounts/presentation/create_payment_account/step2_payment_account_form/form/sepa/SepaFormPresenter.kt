package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.sepa

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.CreateSepaAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.CreateSepaAccountPayload
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui.CountryPickerItem
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.SepaFormUiAction
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.data.replicated.common.validation.SepaPaymentAccountValidation
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.main.MainPresenter

open class SepaFormPresenter(
    mainPresenter: MainPresenter,
) : AccountFormPresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(SepaFormUiState())
    override val uiState: StateFlow<SepaFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SepaFormEffect>()
    val effect = _effect.asSharedFlow()

    private var supportedCountries: HashSet<Country> = HashSet()

    fun initialize(paymentMethod: FiatPaymentMethod) {
        supportedCountries = paymentMethod.supportedCountries.toHashSet()
        val sortedCountries = supportedCountries.sortedBy { country -> country.name }
        _uiState.update { current ->
            current.copy(
                countries = sortedCountries,
                availableAcceptedCountries = sortedCountries.map { country -> CountryPickerItem(country.code, country.name) },
                selectedAcceptedCountryCodes = sortedCountries.map { country -> country.code }.toSet(),
                ibanEntry =
                    current.ibanEntry.copy(
                        validator = { value -> validateSepaIban(value, sortedCountries.map { country -> country.code }.toSet()) },
                    ),
            )
        }
    }

    override fun onCustomAction(action: AccountFormUiAction) {
        when (action) {
            is SepaFormUiAction.OnCountrySelect -> {
                _uiState.update {
                    it.copy(
                        selectedCountryIndex = action.index,
                        countryErrorMessage = null,
                        ibanEntry = it.ibanEntry.clearError(),
                    )
                }
            }

            is SepaFormUiAction.OnHolderNameChange -> updateEntry { it.copy(holderNameEntry = it.holderNameEntry.updateValue(action.value)) }
            is SepaFormUiAction.OnIbanChange -> updateEntry { it.copy(ibanEntry = it.ibanEntry.updateValue(action.value)) }
            is SepaFormUiAction.OnBicChange -> updateEntry { it.copy(bicEntry = it.bicEntry.updateValue(action.value)) }

            SepaFormUiAction.OnOpenAcceptedCountriesPicker -> {
                _uiState.update { it.copy(isAcceptedCountriesPickerOpen = true) }
            }

            SepaFormUiAction.OnCloseAcceptedCountriesPicker -> {
                _uiState.update {
                    it.copy(
                        isAcceptedCountriesPickerOpen = false,
                        acceptedCountrySearchQuery = "",
                    )
                }
            }

            is SepaFormUiAction.OnAcceptedCountrySearchChange -> {
                _uiState.update { it.copy(acceptedCountrySearchQuery = action.value) }
            }

            is SepaFormUiAction.OnAcceptedCountryToggle -> {
                _uiState.update { current ->
                    val updatedSelection =
                        if (current.selectedAcceptedCountryCodes.contains(action.code)) {
                            current.selectedAcceptedCountryCodes - action.code
                        } else {
                            current.selectedAcceptedCountryCodes + action.code
                        }

                    current.copy(
                        selectedAcceptedCountryCodes = updatedSelection,
                        acceptedCountriesErrorMessage = null,
                    )
                }
            }

            SepaFormUiAction.OnSelectAllAcceptedCountries -> {
                _uiState.update {
                    it.copy(
                        selectedAcceptedCountryCodes = it.availableAcceptedCountries.map { item -> item.code }.toSet(),
                        acceptedCountriesErrorMessage = null,
                    )
                }
            }

            SepaFormUiAction.OnClearAllAcceptedCountries -> {
                _uiState.update { it.copy(selectedAcceptedCountryCodes = emptySet()) }
            }

            else -> Unit
        }
    }

    private fun updateEntry(update: (SepaFormUiState) -> SepaFormUiState) {
        _uiState.update(update)
    }

    override fun onNextClick() {
        var selectedCountry: Country? = null
        var acceptedCountryCodes = emptyList<String>()
        val validatedState =
            _uiState.updateAndGet { current ->
                selectedCountry = current.selectedCountry
                acceptedCountryCodes =
                    supportedCountries
                        .filter { country -> current.selectedAcceptedCountryCodes.contains(country.code) }
                        .sortedBy { country -> country.name }
                        .map { country -> country.code }

                current.copy(
                    countryErrorMessage = validateSelectedCountry(selectedCountry),
                    holderNameEntry = current.holderNameEntry.validate(),
                    ibanEntry = current.ibanEntry.validate().validateIbanMatchesSelectedCountry(selectedCountry),
                    bicEntry = current.bicEntry.validate(),
                    acceptedCountriesErrorMessage = validateAcceptedCountries(acceptedCountryCodes),
                )
            }

        val validatedAccountName = validateUniqueAccountNameEntry()
        val country = selectedCountry

        val isValid =
            country != null &&
                validatedState.holderNameEntry.isValid &&
                validatedState.ibanEntry.isValid &&
                validatedState.bicEntry.isValid &&
                validatedState.acceptedCountriesErrorMessage == null &&
                validatedAccountName.isValid

        if (!isValid) {
            return
        }

        presenterScope.launch {
            _effect.emit(
                SepaFormEffect.NavigateToNextScreen(
                    CreateSepaAccount(
                        accountName = validatedAccountName.value.trim(),
                        accountPayload =
                            CreateSepaAccountPayload(
                                selectedCountryCode = country.code,
                                acceptedCountryCodes = acceptedCountryCodes,
                                holderName = validatedState.holderNameEntry.value.trim(),
                                iban =
                                    validatedState.ibanEntry.value
                                        .trim()
                                        .replace(" ", "")
                                        .uppercase(),
                                bic =
                                    validatedState.bicEntry.value
                                        .trim()
                                        .uppercase(),
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

private fun validateSepaIban(
    value: String,
    sepaCountryCodes: Set<String>,
): String? {
    try {
        SepaPaymentAccountValidation.validateSepaIban(value, sepaCountryCodes)
    } catch (exception: IllegalArgumentException) {
        return exception.message ?: "validation.invalid".i18n()
    }

    return null
}

internal fun validateBic(value: String): String? {
    try {
        SepaPaymentAccountValidation.validateBic(value.trim())
    } catch (exception: IllegalArgumentException) {
        return exception.message ?: "validation.invalid".i18n()
    }

    return null
}

private fun validateSelectedCountry(country: Country?): String? =
    if (country == null) {
        "paymentAccounts.createAccount.accountData.country.error".i18n()
    } else {
        null
    }

private fun validateAcceptedCountries(acceptedCountryCodes: List<String>): String? =
    if (acceptedCountryCodes.isEmpty()) {
        "paymentAccounts.createAccount.accountData.sepa.acceptCountries.error".i18n()
    } else {
        null
    }

private fun DataEntry.validateIbanMatchesSelectedCountry(country: Country?): DataEntry {
    if (errorMessage != null || country == null) {
        return this
    }

    return try {
        SepaPaymentAccountValidation.validateIbanMatchesCountryCode(value.trim(), country.code)
        this
    } catch (_: IllegalArgumentException) {
        withError("validation.ibanCountryMismatch".i18n(value.trim().take(2).uppercase()))
    }
}
