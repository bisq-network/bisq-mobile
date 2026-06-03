package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.sepa

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui.CountryPickerBottomSheet
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui.CountryPickerItem
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui.CountrySummaryRow
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.SepaFormUiAction
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqDropdownSearchable
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
@Composable
fun SepaFormContent(
    presenter: SepaFormPresenter,
    onNavigateToNextScreen: (CreatePaymentAccount) -> Unit,
    paymentMethod: FiatPaymentMethod,
    modifier: Modifier = Modifier,
) {
    val uiState by presenter.uiState.collectAsState()
    val currentOnNavigate by rememberUpdatedState(onNavigateToNextScreen)

    LaunchedEffect(presenter, paymentMethod) {
        presenter.initialize(paymentMethod)
    }

    LaunchedEffect(presenter) {
        presenter.effect.collect { effect ->
            when (effect) {
                is SepaFormEffect.NavigateToNextScreen -> currentOnNavigate(effect.account)
            }
        }
    }

    SepaFormContent(
        uiState = uiState,
        onAction = presenter::onAction,
        modifier = modifier,
    )
}

@ExcludeFromCoverage
@Composable
private fun SepaFormContent(
    uiState: SepaFormUiState,
    onAction: (AccountFormUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SepaCountryDropdown(
            countries = uiState.countries,
            selectedIndex = uiState.selectedCountryIndex,
            errorMessage = uiState.countryErrorMessage,
            onOptionSelect = { index -> onAction(SepaFormUiAction.OnCountrySelect(index)) },
        )

        val holderNameLabel = "paymentAccounts.holderName".i18n()
        SepaTextField(
            entry = uiState.holderNameEntry,
            label = holderNameLabel,
            placeholder = "paymentAccounts.createAccount.prompt".i18n(holderNameLabel.lowercase()),
            onValueChange = { onAction(SepaFormUiAction.OnHolderNameChange(it)) },
        )

        val ibanLabel = "paymentAccounts.sepa.iban".i18n()
        SepaTextField(
            entry = uiState.ibanEntry,
            label = ibanLabel,
            placeholder = "paymentAccounts.createAccount.prompt".i18n(ibanLabel),
            onValueChange = { onAction(SepaFormUiAction.OnIbanChange(it)) },
        )

        val bicLabel = "paymentAccounts.sepa.bic".i18n()
        SepaTextField(
            entry = uiState.bicEntry,
            label = bicLabel,
            placeholder = "paymentAccounts.createAccount.prompt".i18n(bicLabel),
            onValueChange = { onAction(SepaFormUiAction.OnBicChange(it)) },
        )

        BisqGap.V1()
        BisqText.BaseRegular("paymentAccounts.createAccount.accountData.sepa.acceptCountries".i18n())
        BisqGap.VHalf()
        CountrySummaryRow(
            selectedCount = uiState.selectedAcceptedCountryCodes.size,
            totalCount = uiState.availableAcceptedCountries.size,
            isError = uiState.acceptedCountriesErrorMessage != null,
            onClick = { onAction(SepaFormUiAction.OnOpenAcceptedCountriesPicker) },
        )

        uiState.acceptedCountriesErrorMessage?.let { errorMessage ->
            BisqGap.VHalf()
            BisqText.SmallLight(
                text = errorMessage,
                color = BisqTheme.colors.danger,
            )
        }
    }

    if (uiState.isAcceptedCountriesPickerOpen) {
        CountryPickerBottomSheet(
            selectedCountryCodes = uiState.selectedAcceptedCountryCodes,
            countries = uiState.availableAcceptedCountries,
            searchQuery = uiState.acceptedCountrySearchQuery,
            selectedCount = uiState.selectedAcceptedCountryCodes.size,
            totalCount = uiState.availableAcceptedCountries.size,
            onSearchChange = { onAction(SepaFormUiAction.OnAcceptedCountrySearchChange(it)) },
            onToggle = { code -> onAction(SepaFormUiAction.OnAcceptedCountryToggle(code)) },
            onSelectAll = { onAction(SepaFormUiAction.OnSelectAllAcceptedCountries) },
            onClearAll = { onAction(SepaFormUiAction.OnClearAllAcceptedCountries) },
            onDismiss = { onAction(SepaFormUiAction.OnCloseAcceptedCountriesPicker) },
        )
    }
}

@Composable
private fun SepaCountryDropdown(
    countries: List<Country>,
    selectedIndex: Int,
    errorMessage: String?,
    onOptionSelect: (Int) -> Unit,
) {
    BisqDropdownSearchable(
        options = countries.map { country -> country.name },
        selectedIndex = selectedIndex,
        onOptionSelect = onOptionSelect,
        modifier = Modifier.fillMaxWidth(),
        label = "paymentAccounts.createAccount.accountData.country".i18n(),
        prompt = "paymentAccounts.createAccount.accountData.country.prompt".i18n(),
    )

    errorMessage?.let { message ->
        BisqGap.VHalf()
        BisqText.SmallLight(
            text = message,
            color = BisqTheme.colors.danger,
        )
    }
}

@Composable
private fun SepaTextField(
    entry: DataEntry,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    BisqTextFieldV0(
        modifier = modifier.padding(top = 12.dp),
        value = entry.value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        isError = entry.errorMessage != null,
        bottomMessage = entry.errorMessage,
        singleLine = true,
    )
}

@Preview
@Composable
private fun SepaFormContentPreview() {
    BisqTheme.Preview {
        SepaFormContent(
            uiState =
                SepaFormUiState(
                    countries = listOf(Country("DE", "Germany"), Country("FR", "France")),
                    selectedCountryIndex = 0,
                    holderNameEntry = DataEntry(value = "Alice Doe"),
                    ibanEntry = DataEntry(value = "DE89370400440532013000"),
                    bicEntry = DataEntry(value = "DEUTDEFF"),
                    availableAcceptedCountries =
                        listOf(
                            CountryPickerItem("DE", "Germany"),
                            CountryPickerItem("FR", "France"),
                            CountryPickerItem("ES", "Spain"),
                        ),
                    selectedAcceptedCountryCodes = setOf("DE", "FR"),
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SepaFormContentErrorPreview() {
    BisqTheme.Preview {
        SepaFormContent(
            uiState =
                SepaFormUiState(
                    countries = listOf(Country("DE", "Germany"), Country("FR", "France")),
                    countryErrorMessage = "paymentAccounts.createAccount.accountData.country.error".i18n(),
                    holderNameEntry = DataEntry(value = "A", errorMessage = "validation.tooShortOrTooLong".i18n(2, 70)),
                    ibanEntry = DataEntry(value = "bad", errorMessage = "validation.invalid".i18n()),
                    bicEntry = DataEntry(value = "bad", errorMessage = "validation.invalid".i18n()),
                    availableAcceptedCountries = listOf(CountryPickerItem("DE", "Germany")),
                    acceptedCountriesErrorMessage = "paymentAccounts.createAccount.accountData.sepa.acceptCountries.error".i18n(),
                ),
            onAction = {},
        )
    }
}
