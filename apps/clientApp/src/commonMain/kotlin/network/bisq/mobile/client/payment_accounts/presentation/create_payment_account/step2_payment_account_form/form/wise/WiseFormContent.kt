package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise

import androidx.compose.foundation.layout.Column
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
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui.CurrencyPickerBottomSheet
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui.CurrencyPickerItem
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui.CurrencySummaryRow
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
@Composable
fun WiseFormContent(
    presenter: WiseFormPresenter,
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
                is WiseFormEffect.NavigateToNextScreen -> currentOnNavigate(effect.account)
            }
        }
    }

    WiseFormContent(
        uiState = uiState,
        onAction = presenter::onAction,
        modifier = modifier,
    )
}

@ExcludeFromCoverage
@Composable
private fun WiseFormContent(
    uiState: WiseFormUiState,
    onAction: (WiseFormUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BisqTextFieldV0(
            value = uiState.holderNameEntry.value,
            onValueChange = { onAction(WiseFormUiAction.OnHolderNameChange(it)) },
            label = "paymentAccounts.holderName".i18n(),
            placeholder =
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.holderName".i18n().lowercase(),
                ),
            isError = uiState.holderNameEntry.errorMessage != null,
            bottomMessage = uiState.holderNameEntry.errorMessage,
            singleLine = true,
        )

        BisqTextFieldV0(
            modifier = Modifier.padding(top = 12.dp),
            value = uiState.emailEntry.value,
            onValueChange = { onAction(WiseFormUiAction.OnEmailChange(it)) },
            label = "paymentAccounts.email".i18n(),
            placeholder =
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.email".i18n().lowercase(),
                ),
            isError = uiState.emailEntry.errorMessage != null,
            bottomMessage = uiState.emailEntry.errorMessage,
            singleLine = true,
        )

        BisqGap.V1()

        CurrencySummaryRow(
            selectedCount = uiState.selectedCurrencyCodes.size,
            totalCount = uiState.availableCurrencies.size,
            isError = uiState.currencyErrorMessage != null,
            onClick = { onAction(WiseFormUiAction.OnOpenCurrencyPicker) },
        )

        uiState.currencyErrorMessage?.let { errorMessage ->
            BisqGap.VHalf()
            BisqText.SmallLight(
                text = errorMessage,
                color = BisqTheme.colors.danger,
            )
        }
    }

    if (uiState.isCurrencyPickerOpen) {
        CurrencyPickerBottomSheet(
            selectedCurrencyCodes = uiState.selectedCurrencyCodes,
            currencies = uiState.availableCurrencies,
            searchQuery = uiState.currencySearchQuery,
            selectedCount = uiState.selectedCurrencyCodes.size,
            totalCount = uiState.availableCurrencies.size,
            onSearchChange = { onAction(WiseFormUiAction.OnCurrencySearchChange(it)) },
            onToggle = { code -> onAction(WiseFormUiAction.OnCurrencyToggle(code)) },
            onSelectAll = { onAction(WiseFormUiAction.OnSelectAllCurrencies) },
            onClearAll = { onAction(WiseFormUiAction.OnClearAllCurrencies) },
            onDismiss = { onAction(WiseFormUiAction.OnCloseCurrencyPicker) },
        )
    }
}

@Preview
@Composable
private fun WiseFormContent_DefaultPreview() {
    BisqTheme.Preview {
        WiseFormContent(
            uiState =
                WiseFormUiState(
                    holderNameEntry = DataEntry(value = "Satoshi Nakamoto"),
                    emailEntry = DataEntry(value = "satoshi@example.com"),
                    availableCurrencies =
                        listOf(
                            CurrencyPickerItem("USD", "USD (US Dollar)"),
                            CurrencyPickerItem("EUR", "EUR (Euro)"),
                            CurrencyPickerItem("GBP", "GBP (British Pound)"),
                        ),
                    selectedCurrencyCodes = setOf("USD", "EUR"),
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun WiseFormContent_ErrorPreview() {
    BisqTheme.Preview {
        WiseFormContent(
            uiState =
                WiseFormUiState(
                    holderNameEntry = DataEntry(value = "a", errorMessage = "validation.tooShortOrTooLong".i18n(3, 100)),
                    emailEntry = DataEntry(value = "bad-email", errorMessage = "validation.invalidEmail".i18n()),
                    availableCurrencies =
                        listOf(
                            CurrencyPickerItem("USD", "USD (US Dollar)"),
                            CurrencyPickerItem("EUR", "EUR (Euro)"),
                        ),
                    selectedCurrencyCodes = emptySet(),
                    currencyErrorMessage = "mobile.paymentAccounts.currencyPicker.error".i18n(),
                ),
            onAction = {},
        )
    }
}
