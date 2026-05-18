package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.wise

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
import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.create_payment_account.core.util.fiatCurrencyCodeNameToDisplayStringFormat
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.WiseFormUiAction
import network.bisq.mobile.presentation.main.MainPresenter

open class WiseFormPresenter(
    mainPresenter: MainPresenter,
) : AccountFormPresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(WiseFormUiState())
    override val uiState: StateFlow<WiseFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<WiseFormEffect>()
    val effect = _effect.asSharedFlow()

    private var supportedCurrencies: HashSet<FiatCurrency> = HashSet()

    fun initialize(paymentMethod: FiatPaymentMethod) {
        supportedCurrencies = paymentMethod.supportedCurrencies.toHashSet()
        _uiState.update { currentState ->
            currentState.copy(
                availableCurrencies = supportedCurrencies.sortedBy { currency -> currency.code }.map { currency -> WiseCurrencyItem(currency.code, fiatCurrencyCodeNameToDisplayStringFormat(currency.code, currency.name)) },
                selectedCurrencyCodes = supportedCurrencies.map { currency -> currency.code }.toSet(),
            )
        }
    }

    override fun onCustomAction(action: AccountFormUiAction) {
        when (action) {
            is WiseFormUiAction.OnHolderNameChange -> {
                _uiState.update {
                    it.copy(
                        holderNameEntry = it.holderNameEntry.updateValue(action.value),
                    )
                }
            }

            is WiseFormUiAction.OnEmailChange -> {
                _uiState.update {
                    it.copy(
                        emailEntry = it.emailEntry.updateValue(action.value),
                    )
                }
            }

            WiseFormUiAction.OnOpenCurrencyPicker -> {
                _uiState.update {
                    it.copy(
                        isCurrencyPickerOpen = true,
                    )
                }
            }

            WiseFormUiAction.OnCloseCurrencyPicker -> {
                _uiState.update {
                    it.copy(
                        isCurrencyPickerOpen = false,
                        currencySearchQuery = "",
                    )
                }
            }

            is WiseFormUiAction.OnCurrencySearchChange -> {
                _uiState.update {
                    it.copy(
                        currencySearchQuery = action.value,
                    )
                }
            }

            is WiseFormUiAction.OnCurrencyToggle -> {
                _uiState.update { current ->
                    val updatedSelection =
                        if (current.selectedCurrencyCodes.contains(action.code)) {
                            current.selectedCurrencyCodes - action.code
                        } else {
                            current.selectedCurrencyCodes + action.code
                        }

                    current.copy(
                        selectedCurrencyCodes = updatedSelection,
                        currencyErrorMessage = null,
                    )
                }
            }

            WiseFormUiAction.OnSelectAllCurrencies -> {
                _uiState.update {
                    it.copy(
                        selectedCurrencyCodes = it.availableCurrencies.map { item -> item.code }.toSet(),
                        currencyErrorMessage = null,
                    )
                }
            }

            WiseFormUiAction.OnClearAllCurrencies -> {
                _uiState.update {
                    it.copy(
                        selectedCurrencyCodes = emptySet(),
                    )
                }
            }

            else -> Unit
        }
    }

    override fun onNextClick() {
        var selectedCurrencies = emptyList<FiatCurrency>()
        val validatedState =
            _uiState.updateAndGet {
                selectedCurrencies =
                    supportedCurrencies
                        .filter { currency -> it.selectedCurrencyCodes.contains(currency.code) }
                        .sortedBy { currency -> currency.code }

                it.copy(
                    holderNameEntry = it.holderNameEntry.validate(),
                    emailEntry = it.emailEntry.validate(),
                    currencyErrorMessage = validateSelectedCurrencies(selectedCurrencies),
                )
            }

        val validatedAccountName = validateUniqueAccountNameEntry()

        val isValid =
            validatedState.holderNameEntry.isValid &&
                validatedState.emailEntry.isValid &&
                validatedState.currencyErrorMessage == null &&
                validatedAccountName.isValid

        if (!isValid) {
            return
        }

        presenterScope.launch {
            _effect.emit(
                WiseFormEffect.NavigateToNextScreen(
                    CreateWiseAccount(
                        accountName = validatedAccountName.value.trim(),
                        accountPayload =
                            CreateWiseAccountPayload(
                                selectedCurrencies = selectedCurrencies,
                                holderName = validatedState.holderNameEntry.value.trim(),
                                email = validatedState.emailEntry.value.trim(),
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

internal fun validateEmail(value: String): String? =
    if (EmailValidation.isValid(value.trim())) {
        null
    } else {
        "validation.invalidEmail".i18n()
    }

internal fun validateSelectedCurrencies(selectedCurrencies: List<FiatCurrency>): String? =
    if (selectedCurrencies.isEmpty()) {
        "mobile.paymentAccounts.wise.currencies.error".i18n()
    } else {
        null
    }
