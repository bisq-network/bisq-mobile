package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.cash_deposit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.payment_accounts.presentation.common.util.toDisplayString
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.CashDepositFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.ui.PaymentMethodBackgroundInformationDialog
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.fiat.BankAccountType
import network.bisq.mobile.domain.model.account.fiat.Country
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
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
fun CashDepositFormContent(
    presenter: CashDepositFormPresenter,
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
                is CashDepositFormEffect.NavigateToNextScreen -> currentOnNavigate(effect.account)
            }
        }
    }

    CashDepositFormContent(
        uiState = uiState,
        onAction = presenter::onAction,
        modifier = modifier,
    )
}

@ExcludeFromCoverage
@Composable
private fun CashDepositFormContent(
    uiState: CashDepositFormUiState,
    onAction: (AccountFormUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isInPreview = LocalInspectionMode.current
    val (showBackgroundInformationDialog, setShowBackgroundInformationDialog) =
        rememberSaveable { mutableStateOf(!isInPreview) }

    Column(modifier = modifier) {
        CashDepositDropdown(
            options = uiState.countries.map { country -> country.name },
            selectedIndex = uiState.selectedCountryIndex,
            label = "paymentAccounts.country".i18n(),
            prompt = "paymentAccounts.createAccount.accountData.country.prompt".i18n(),
            errorMessage = uiState.countryErrorMessage,
            onOptionSelect = { index -> onAction(CashDepositFormUiAction.OnCountrySelect(index)) },
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
                CashDepositDropdown(
                    options = uiState.currencies.map { currency -> currency.toDisplayString() },
                    selectedIndex = uiState.selectedCurrencyIndex,
                    label = "paymentAccounts.currency".i18n(),
                    prompt = "paymentAccounts.createAccount.accountData.currency.prompt".i18n(),
                    errorMessage = uiState.currencyErrorMessage,
                    onOptionSelect = { index -> onAction(CashDepositFormUiAction.OnCurrencySelect(index)) },
                    searchable = true,
                )

                BisqGap.V1()
                val holderNameLabel = "paymentAccounts.holderName".i18n()
                CashDepositTextField(
                    entry = uiState.holderNameEntry,
                    label = holderNameLabel,
                    placeholder = "paymentAccounts.createAccount.prompt".i18n(holderNameLabel.lowercase()),
                    onValueChange = { onAction(CashDepositFormUiAction.OnHolderNameChange(it)) },
                )

                if (uiState.countryDetails.holderIdRequired) {
                    CashDepositTextField(
                        entry = uiState.holderIdEntry,
                        label = uiState.countryDetails.holderIdDescription,
                        placeholder = uiState.countryDetails.holderIdDescription,
                        onValueChange = { onAction(CashDepositFormUiAction.OnHolderIdChange(it)) },
                    )
                }

                val bankNameLabel = "paymentAccounts.bank.bankName".i18n()
                CashDepositTextField(
                    entry = uiState.bankNameEntry,
                    label = bankNameLabel,
                    placeholder = "paymentAccounts.createAccount.prompt".i18n(bankNameLabel.lowercase()),
                    onValueChange = { onAction(CashDepositFormUiAction.OnBankNameChange(it)) },
                )

                if (uiState.countryDetails.bankIdRequired) {
                    CashDepositTextField(
                        entry = uiState.bankIdEntry,
                        label = uiState.countryDetails.bankIdDescription,
                        placeholder = uiState.countryDetails.bankIdDescription,
                        onValueChange = { onAction(CashDepositFormUiAction.OnBankIdChange(it)) },
                    )
                }

                if (uiState.countryDetails.branchIdRequired) {
                    CashDepositTextField(
                        entry = uiState.branchIdEntry,
                        label = uiState.countryDetails.branchIdDescription,
                        placeholder = uiState.countryDetails.branchIdDescription,
                        onValueChange = { onAction(CashDepositFormUiAction.OnBranchIdChange(it)) },
                    )
                }

                CashDepositTextField(
                    entry = uiState.accountNrEntry,
                    label = uiState.countryDetails.accountNrDescription,
                    placeholder = uiState.countryDetails.accountNrDescription,
                    onValueChange = { onAction(CashDepositFormUiAction.OnAccountNrChange(it)) },
                )

                if (uiState.countryDetails.bankAccountTypeRequired) {
                    CashDepositBankAccountTypeDropdown(
                        selectedBankAccountType = uiState.selectedBankAccountType,
                        errorMessage = uiState.bankAccountTypeErrorMessage,
                        onOptionSelect = { type -> onAction(CashDepositFormUiAction.OnBankAccountTypeSelect(type)) },
                    )
                }

                if (uiState.countryDetails.nationalAccountIdRequired) {
                    CashDepositTextField(
                        entry = uiState.nationalAccountIdEntry,
                        label = uiState.countryDetails.nationalAccountIdDescription,
                        placeholder = uiState.countryDetails.nationalAccountIdDescription,
                        onValueChange = { onAction(CashDepositFormUiAction.OnNationalAccountIdChange(it)) },
                    )
                }

                val requirementsLabel = "paymentAccounts.cashDeposit.requirements".i18n()
                CashDepositTextField(
                    entry = uiState.requirementsEntry,
                    label = requirementsLabel,
                    placeholder = "paymentAccounts.createAccount.prompt".i18n(requirementsLabel.lowercase()),
                    onValueChange = { onAction(CashDepositFormUiAction.OnRequirementsChange(it)) },
                    singleLine = false,
                    minLines = 3,
                )
            }
        }
    }

    if (showBackgroundInformationDialog) {
        PaymentMethodBackgroundInformationDialog(
            bodyText = "paymentAccounts.createAccount.accountData.backgroundOverlay.cashDeposit".i18n(),
            onDismissRequest = { setShowBackgroundInformationDialog(false) },
        )
    }
}

@Composable
private fun CashDepositDropdown(
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
private fun CashDepositBankAccountTypeDropdown(
    selectedBankAccountType: BankAccountType?,
    errorMessage: String?,
    onOptionSelect: (BankAccountType) -> Unit,
) {
    BisqGap.V1()
    CashDepositDropdown(
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
private fun CashDepositTextField(
    entry: DataEntry,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    BisqTextFieldV0(
        modifier = modifier.padding(top = 12.dp),
        value = entry.value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        isError = entry.errorMessage != null,
        bottomMessage = entry.errorMessage,
        singleLine = singleLine,
        minLines = minLines,
    )
}

private fun BankAccountType.toDisplayString(): String =
    when (this) {
        BankAccountType.CHECKING -> "paymentAccounts.bank.bankAccountType.CHECKINGS".i18n()
        BankAccountType.SAVINGS -> "paymentAccounts.bank.bankAccountType.SAVINGS".i18n()
    }

@Preview
@Composable
private fun CashDepositFormContentCountryOnlyPreview() {
    BisqTheme.Preview {
        CashDepositFormContent(
            uiState =
                CashDepositFormUiState(
                    countries = listOf(Country("US", "United States"), Country("DE", "Germany")),
                    currencies = listOf(FiatCurrency("USD", "US Dollar")),
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun CashDepositFormContentLoadingPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.height(220.dp)) {
            CashDepositFormContent(
                uiState =
                    CashDepositFormUiState(
                        countries = listOf(Country("US", "United States")),
                        selectedCountryIndex = 0,
                        isLoadingCountryDetails = true,
                    ),
                onAction = {},
            )
        }
    }
}
