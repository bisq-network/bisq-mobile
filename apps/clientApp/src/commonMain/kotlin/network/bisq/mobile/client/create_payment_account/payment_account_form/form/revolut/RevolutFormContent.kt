package network.bisq.mobile.client.create_payment_account.payment_account_form.form.revolut

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.client.create_payment_account.core.ui.CurrencyPickerBottomSheet
import network.bisq.mobile.client.create_payment_account.core.ui.CurrencyPickerItem
import network.bisq.mobile.client.create_payment_account.core.ui.CurrencySummaryRow
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.action.RevolutFormUiAction
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
@Composable
fun RevolutFormContent(
    presenter: RevolutFormPresenter,
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
                is RevolutFormEffect.NavigateToNextScreen -> currentOnNavigate(effect.account)
            }
        }
    }

    RevolutFormContent(
        uiState = uiState,
        onAction = presenter::onAction,
        modifier = modifier,
    )
}

@ExcludeFromCoverage
@Composable
private fun RevolutFormContent(
    uiState: RevolutFormUiState,
    onAction: (AccountFormUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredCurrencies =
        remember(uiState.availableCurrencies, uiState.currencySearchQuery) {
            filterCurrencies(
                uiState.availableCurrencies,
                uiState.currencySearchQuery,
            )
        }

    Column(modifier = modifier) {
        BisqTextFieldV0(
            value = uiState.userNameEntry.value,
            onValueChange = { onAction(RevolutFormUiAction.OnUserNameChange(it)) },
            label = "paymentAccounts.userName".i18n(),
            placeholder =
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.userName".i18n().lowercase(),
                ),
            isError = uiState.userNameEntry.errorMessage != null,
            bottomMessage = uiState.userNameEntry.errorMessage,
            singleLine = true,
        )

        BisqGap.V1()

        CurrencySummaryRow(
            selectedCount = uiState.selectedCurrencyCodes.size,
            totalCount = uiState.availableCurrencies.size,
            isError = uiState.currencyErrorMessage != null,
            onClick = { onAction(RevolutFormUiAction.OnOpenCurrencyPicker) },
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
            currencies = filteredCurrencies,
            searchQuery = uiState.currencySearchQuery,
            selectedCount = uiState.selectedCurrencyCodes.size,
            totalCount = uiState.availableCurrencies.size,
            onSearchChange = { onAction(RevolutFormUiAction.OnCurrencySearchChange(it)) },
            onToggle = { code -> onAction(RevolutFormUiAction.OnCurrencyToggle(code)) },
            onSelectAll = { onAction(RevolutFormUiAction.OnSelectAllCurrencies) },
            onClearAll = { onAction(RevolutFormUiAction.OnClearAllCurrencies) },
            onDismiss = { onAction(RevolutFormUiAction.OnCloseCurrencyPicker) },
        )
    }
}

private fun filterCurrencies(
    currencies: List<CurrencyPickerItem>,
    query: String,
): List<CurrencyPickerItem> {
    if (query.isBlank()) {
        return currencies
    }

    return currencies.filter { currency ->
        currency.code.contains(query, ignoreCase = true) ||
            currency.displayName.contains(query, ignoreCase = true)
    }
}

@Preview
@Composable
private fun RevolutFormContentPreview_DefaultPreview() {
    BisqTheme.Preview {
        RevolutFormContent(
            uiState =
                RevolutFormUiState(
                    userNameEntry = DataEntry(value = "satoshi"),
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
private fun RevolutFormContentPreview_ErrorPreview() {
    BisqTheme.Preview {
        RevolutFormContent(
            uiState =
                RevolutFormUiState(
                    userNameEntry = DataEntry(value = "a", errorMessage = "validation.tooShortOrTooLong".i18n(2, 70)),
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
