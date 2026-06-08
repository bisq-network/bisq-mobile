package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.presentation.common.util.toDisplayString
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqDropdown
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqDropdownSearchable
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
@Composable
fun <T : CreatePaymentAccount> BankAccountFormContent(
    presenter: BankAccountFormPresenter<T>,
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
                is BankAccountFormEffect.NavigateToNextScreen -> currentOnNavigate(effect.account)
            }
        }
    }

    BankAccountFormContent(
        uiState = uiState,
        onAction = presenter::onAction,
        modifier = modifier,
    )
}

@ExcludeFromCoverage
@Composable
private fun BankAccountFormContent(
    uiState: BankAccountFormUiState,
    onAction: (BankAccountFormUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BankAccountDropdown(
            options = uiState.countries.map { country -> country.name },
            selectedIndex = uiState.selectedCountryIndex,
            label = "paymentAccounts.country".i18n(),
            prompt = "paymentAccounts.createAccount.accountData.country.prompt".i18n(),
            errorMessage = uiState.countryErrorMessage,
            onOptionSelect = { index -> onAction(BankAccountFormUiAction.OnCountrySelect(index)) },
            searchable = true,
        )

        when {
            uiState.isLoadingCountryDetails -> {
                LoadingState(
                    paddingValues = PaddingValues(vertical = 32.dp),
                )
            }

            uiState.isCountryDetailsError -> {
                ErrorState(
                    message = "mobile.error.generic".i18n(),
                    paddingValues = PaddingValues(vertical = 24.dp),
                )
            }

            uiState.countryDetails != null -> {
                BisqGap.V1()
                BankAccountDropdown(
                    options = uiState.currencies.map { currency -> currency.toDisplayString() },
                    selectedIndex = uiState.selectedCurrencyIndex,
                    label = "paymentAccounts.currency".i18n(),
                    prompt = "paymentAccounts.createAccount.accountData.currency.prompt".i18n(),
                    errorMessage = uiState.currencyErrorMessage,
                    onOptionSelect = { index -> onAction(BankAccountFormUiAction.OnCurrencySelect(index)) },
                    searchable = true,
                )

                val details = uiState.countryDetails
                val bankAccountValidationSupported = details.bankAccountValidationSupported

                if (bankAccountValidationSupported) {
                    BisqGap.V1()
                    val holderNameLabel = "paymentAccounts.holderName".i18n()
                    BankAccountTextField(
                        entry = uiState.holderNameEntry,
                        label = holderNameLabel,
                        placeholder = "paymentAccounts.createAccount.prompt".i18n(holderNameLabel.lowercase()),
                        onValueChange = { onAction(BankAccountFormUiAction.OnHolderNameChange(it)) },
                    )

                    if (details.holderIdRequired) {
                        BankAccountTextField(
                            entry = uiState.holderIdEntry,
                            label = details.holderIdDescription,
                            placeholder = details.holderIdDescription,
                            onValueChange = { onAction(BankAccountFormUiAction.OnHolderIdChange(it)) },
                        )
                    }

                    if (details.bankNameRequired) {
                        val bankNameLabel = "paymentAccounts.bank.bankName".i18n()
                        BankAccountTextField(
                            entry = uiState.bankNameEntry,
                            label = bankNameLabel,
                            placeholder = "paymentAccounts.createAccount.prompt".i18n(bankNameLabel.lowercase()),
                            onValueChange = { onAction(BankAccountFormUiAction.OnBankNameChange(it)) },
                        )
                    }

                    if (details.bankIdRequired) {
                        BankAccountTextField(
                            entry = uiState.bankIdEntry,
                            label = details.bankIdDescription,
                            placeholder = details.bankIdDescription,
                            onValueChange = { onAction(BankAccountFormUiAction.OnBankIdChange(it)) },
                        )
                    }

                    if (details.branchIdRequired) {
                        BankAccountTextField(
                            entry = uiState.branchIdEntry,
                            label = details.branchIdDescription,
                            placeholder = details.branchIdDescription,
                            onValueChange = { onAction(BankAccountFormUiAction.OnBranchIdChange(it)) },
                        )
                    }
                }

                BankAccountTextField(
                    entry = uiState.accountNrEntry,
                    label = details.accountNrDescription,
                    placeholder = details.accountNrDescription,
                    onValueChange = { onAction(BankAccountFormUiAction.OnAccountNrChange(it)) },
                )

                if (bankAccountValidationSupported && details.bankAccountTypeRequired) {
                    BankAccountTypeDropdown(
                        selectedBankAccountType = uiState.selectedBankAccountType,
                        errorMessage = uiState.bankAccountTypeErrorMessage,
                        onOptionSelect = { type -> onAction(BankAccountFormUiAction.OnBankAccountTypeSelect(type)) },
                    )
                }

                if (bankAccountValidationSupported && details.nationalAccountIdRequired) {
                    BankAccountTextField(
                        entry = uiState.nationalAccountIdEntry,
                        label = details.nationalAccountIdDescription,
                        placeholder = details.nationalAccountIdDescription,
                        onValueChange = { onAction(BankAccountFormUiAction.OnNationalAccountIdChange(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BankAccountDropdown(
    options: List<String>,
    selectedIndex: Int,
    label: String,
    errorMessage: String?,
    onOptionSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    prompt: String? = null,
    searchable: Boolean = false,
) {
    if (searchable) {
        BisqDropdownSearchable(
            options = options,
            selectedIndex = selectedIndex,
            onOptionSelect = onOptionSelect,
            modifier = modifier.fillMaxWidth(),
            label = label,
            prompt = prompt,
        )
    } else {
        BisqDropdown(
            options = options,
            selectedIndex = selectedIndex,
            onOptionSelect = onOptionSelect,
            modifier = modifier.fillMaxWidth(),
            label = label,
            prompt = prompt,
        )
    }

    errorMessage?.let { message ->
        BisqGap.VHalf()
        BisqText.SmallLight(
            text = message,
            color = BisqTheme.colors.danger,
        )
    }
}

@Composable
private fun BankAccountTypeDropdown(
    selectedBankAccountType: BankAccountType?,
    errorMessage: String?,
    onOptionSelect: (BankAccountType) -> Unit,
) {
    BisqGap.V1()
    BankAccountDropdown(
        options = BankAccountType.entries.map { type -> type.toDisplayString() },
        selectedIndex = BankAccountType.entries.indexOf(selectedBankAccountType),
        label = "paymentAccounts.bank.bankAccountType".i18n(),
        prompt = "paymentAccounts.createAccount.accountData.bank.bankAccountType.prompt".i18n(),
        errorMessage = errorMessage,
        onOptionSelect = { index ->
            BankAccountType.entries.getOrNull(index)?.let(onOptionSelect)
        },
    )
}

@Composable
private fun BankAccountTextField(
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
private fun BankAccountFormContentCountryOnlyPreview() {
    BisqTheme.Preview {
        BankAccountFormContent(
            uiState =
                BankAccountFormUiState(
                    countries = listOf(Country("US", "United States"), Country("DE", "Germany")),
                    currencies = listOf(FiatCurrency("USD", "US Dollar")),
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun BankAccountFormContentLoadingPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.height(220.dp)) {
            BankAccountFormContent(
                uiState =
                    BankAccountFormUiState(
                        countries = listOf(Country("US", "United States")),
                        selectedCountryIndex = 0,
                        isLoadingCountryDetails = true,
                    ),
                onAction = {},
            )
        }
    }
}
