package network.bisq.mobile.client.create_payment_account.payment_account_form.form.cash_deposit

import network.bisq.mobile.client.create_payment_account.payment_account_form.form.AccountFormUiState
import network.bisq.mobile.domain.model.account.fiat.BankAccountCountryDetails
import network.bisq.mobile.domain.model.account.fiat.BankAccountType
import network.bisq.mobile.domain.model.account.fiat.Country
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.presentation.common.ui.utils.DataEntry

private const val NO_SELECTION_INDEX = -1

data class CashDepositFormUiState(
    val countries: List<Country> = emptyList(),
    val currencies: List<FiatCurrency> = emptyList(),
    val selectedCountryIndex: Int = NO_SELECTION_INDEX,
    val selectedCurrencyIndex: Int = NO_SELECTION_INDEX,
    val countryErrorMessage: String? = null,
    val currencyErrorMessage: String? = null,
    val countryDetails: BankAccountCountryDetails? = null,
    val isLoadingCountryDetails: Boolean = false,
    val isCountryDetailsError: Boolean = false,
    val holderNameEntry: DataEntry = DataEntry(validator = ::validateHolderName),
    val holderIdEntry: DataEntry = DataEntry(validator = ::validateHolderId),
    val bankNameEntry: DataEntry = DataEntry(validator = ::validateBankName),
    val bankIdEntry: DataEntry = DataEntry(validator = ::validateBankId),
    val branchIdEntry: DataEntry = DataEntry(validator = ::validateBranchId),
    val accountNrEntry: DataEntry = DataEntry(validator = ::validateAccountNr),
    val selectedBankAccountType: BankAccountType? = null,
    val bankAccountTypeErrorMessage: String? = null,
    val nationalAccountIdEntry: DataEntry = DataEntry(validator = ::validateNationalAccountId),
    val requirementsEntry: DataEntry = DataEntry(validator = ::validateCashDepositRequirements),
) : AccountFormUiState {
    val selectedCountry: Country?
        get() = countries.getOrNull(selectedCountryIndex)

    val selectedCurrency: FiatCurrency?
        get() = currencies.getOrNull(selectedCurrencyIndex)
}
