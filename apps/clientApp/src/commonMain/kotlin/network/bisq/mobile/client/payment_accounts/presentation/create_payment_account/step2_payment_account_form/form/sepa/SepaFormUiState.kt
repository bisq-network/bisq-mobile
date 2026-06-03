package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.sepa

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui.CountryPickerItem
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiState
import network.bisq.mobile.domain.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.DataEntry

data class SepaFormUiState(
    val countries: List<Country> = emptyList(),
    val selectedCountryIndex: Int = -1,
    val countryErrorMessage: String? = null,
    val holderNameEntry: DataEntry = DataEntry(validator = ::validateHolderName),
    val ibanEntry: DataEntry = DataEntry(),
    val bicEntry: DataEntry = DataEntry(validator = ::validateBic),
    val selectedAcceptedCountryCodes: Set<String> = emptySet(),
    val availableAcceptedCountries: List<CountryPickerItem> = emptyList(),
    val acceptedCountriesErrorMessage: String? = null,
    val isAcceptedCountriesPickerOpen: Boolean = false,
    val acceptedCountrySearchQuery: String = EMPTY_STRING,
) : AccountFormUiState {
    val selectedCountry: Country?
        get() = countries.getOrNull(selectedCountryIndex)
}
