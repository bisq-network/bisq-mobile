package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise

import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui.CurrencyPickerItem
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiState
import network.bisq.mobile.domain.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.DataEntry

data class WiseFormUiState(
    val holderNameEntry: DataEntry = DataEntry(validator = ::validateHolderName),
    val emailEntry: DataEntry = DataEntry(validator = ::validateEmail),
    val selectedCurrencyCodes: Set<String> = emptySet(),
    val availableCurrencies: List<CurrencyPickerItem> = emptyList(),
    val currencyErrorMessage: String? = null,
    val isCurrencyPickerOpen: Boolean = false,
    val currencySearchQuery: String = EMPTY_STRING,
) : AccountFormUiState
