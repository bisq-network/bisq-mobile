package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.revolut

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import network.bisq.mobile.client.payment_accounts.presentation.common.util.toDisplayString
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui.CurrencyPickerItem
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.RevolutFormUiAction
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.domain.model.account.create.fiat.CreateRevolutAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateRevolutAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.main.MainPresenter

open class RevolutFormPresenter(
    mainPresenter: MainPresenter,
) : AccountFormPresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(RevolutFormUiState())
    override val uiState: StateFlow<RevolutFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<RevolutFormEffect>()
    val effect = _effect.asSharedFlow()

    private var supportedCurrencies: HashSet<FiatCurrency> = HashSet()

    fun initialize(paymentMethod: FiatPaymentMethod) {
        supportedCurrencies = paymentMethod.supportedCurrencies.toHashSet()
        _uiState.update {
            it.copy(
                availableCurrencies =
                    supportedCurrencies.sortedBy { currency -> currency.code }.map { currency ->
                        CurrencyPickerItem(
                            code = currency.code,
                            displayName = currency.toDisplayString(),
                        )
                    },
                selectedCurrencyCodes = supportedCurrencies.map { currency -> currency.code }.toSet(),
            )
        }
    }

    override fun onCustomAction(action: AccountFormUiAction) {
        when (action) {
            is RevolutFormUiAction.OnUserNameChange -> {
                _uiState.update {
                    it.copy(userNameEntry = it.userNameEntry.updateValue(action.value))
                }
            }

            RevolutFormUiAction.OnOpenCurrencyPicker -> {
                _uiState.update { it.copy(isCurrencyPickerOpen = true) }
            }

            RevolutFormUiAction.OnCloseCurrencyPicker -> {
                _uiState.update {
                    it.copy(
                        isCurrencyPickerOpen = false,
                        currencySearchQuery = "",
                    )
                }
            }

            is RevolutFormUiAction.OnCurrencySearchChange -> {
                _uiState.update { it.copy(currencySearchQuery = action.value) }
            }

            is RevolutFormUiAction.OnCurrencyToggle -> {
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

            RevolutFormUiAction.OnSelectAllCurrencies -> {
                _uiState.update {
                    it.copy(
                        selectedCurrencyCodes = it.availableCurrencies.map { item -> item.code }.toSet(),
                        currencyErrorMessage = null,
                    )
                }
            }

            RevolutFormUiAction.OnClearAllCurrencies -> {
                _uiState.update { it.copy(selectedCurrencyCodes = emptySet()) }
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
                    userNameEntry = it.userNameEntry.validate(),
                    currencyErrorMessage = validateSelectedCurrencies(selectedCurrencies),
                )
            }

        val validatedAccountName = validateUniqueAccountNameEntry()

        val isValid =
            validatedState.userNameEntry.isValid &&
                validatedState.currencyErrorMessage == null &&
                validatedAccountName.isValid

        if (!isValid) {
            return
        }

        presenterScope.launch {
            _effect.emit(
                RevolutFormEffect.NavigateToNextScreen(
                    CreateRevolutAccount(
                        accountName = validatedAccountName.value.trim(),
                        accountPayload =
                            CreateRevolutAccountPayload(
                                userName = validatedState.userNameEntry.value.trim(),
                                selectedCurrencies = selectedCurrencies,
                            ),
                    ),
                ),
            )
        }
    }
}

internal fun validateUserName(value: String): String? {
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

internal fun validateSelectedCurrencies(selectedCurrencies: List<FiatCurrency>): String? =
    if (selectedCurrencies.isEmpty()) {
        "mobile.paymentAccounts.currencyPicker.error".i18n()
    } else {
        null
    }
