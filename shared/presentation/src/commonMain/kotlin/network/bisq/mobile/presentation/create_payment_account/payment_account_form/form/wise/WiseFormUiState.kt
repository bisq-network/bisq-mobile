package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.wise

import network.bisq.mobile.domain.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.AccountFormUiState

data class WiseFormUiState(
    val holderNameEntry: DataEntry = DataEntry(validator = ::validateHolderName),
    val emailEntry: DataEntry = DataEntry(validator = ::validateEmail),
    val selectedCurrencyCodes: Set<String> = emptySet(),
    val availableCurrencies: List<WiseCurrencyItem> = emptyList(),
    val currencyErrorMessage: String? = null,
    val isCurrencyPickerOpen: Boolean = false,
    val currencySearchQuery: String = EMPTY_STRING,
) : AccountFormUiState

data class WiseCurrencyItem(
    val code: String,
    val displayName: String,
)
