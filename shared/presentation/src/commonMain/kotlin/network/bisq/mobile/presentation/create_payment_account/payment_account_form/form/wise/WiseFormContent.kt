package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.wise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.WiseFormUiAction

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

        WiseCurrencySummaryRow(
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
        WiseCurrencyPickerBottomSheet(
            selectedCurrencyCodes = uiState.selectedCurrencyCodes,
            currencies = filteredCurrencies,
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

@Composable
private fun WiseCurrencySummaryRow(
    selectedCount: Int,
    totalCount: Int,
    isError: Boolean,
    onClick: () -> Unit,
) {
    val summaryText =
        if (selectedCount == totalCount && totalCount > 0) {
            "mobile.paymentAccounts.wise.currencies.allSelected".i18n(totalCount)
        } else {
            "mobile.paymentAccounts.wise.currencies.summary".i18n(selectedCount, totalCount)
        }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey50,
    ) {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
        ) {
            BisqText.SmallLight(
                text = summaryText,
                color = if (isError) BisqTheme.colors.danger else BisqTheme.colors.white,
            )
            BisqText.SmallLight(
                text = "mobile.paymentAccounts.wise.currencies.editLabel".i18n(),
                color = if (isError) BisqTheme.colors.danger else BisqTheme.colors.mid_grey20,
            )
        }
    }
}

@Composable
private fun WiseCurrencyPickerBottomSheet(
    selectedCurrencyCodes: Set<String>,
    currencies: List<WiseCurrencyItem>,
    searchQuery: String,
    selectedCount: Int,
    totalCount: Int,
    onSearchChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    BisqBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H5Regular("mobile.paymentAccounts.wise.picker.title".i18n())
            BisqGap.VHalf()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(onClick = onSelectAll, color = BisqTheme.colors.transparent) {
                        BisqText.SmallRegular(
                            text = "mobile.paymentAccounts.wise.picker.selectAll".i18n(),
                            color = BisqTheme.colors.primary,
                            modifier = Modifier.padding(4.dp),
                        )
                    }

                    Surface(onClick = onClearAll, color = BisqTheme.colors.transparent) {
                        BisqText.SmallRegular(
                            text = "mobile.paymentAccounts.wise.picker.clearAll".i18n(),
                            color = BisqTheme.colors.mid_grey20,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }

                BisqText.SmallLight(
                    text = "mobile.paymentAccounts.wise.currencies.summary".i18n(selectedCount, totalCount),
                    color = BisqTheme.colors.mid_grey20,
                )
            }

            BisqGap.V1()

            BisqSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "mobile.paymentAccounts.wise.picker.searchHint".i18n(),
            )

            BisqGap.V1()

            if (currencies.isEmpty()) {
                BisqText.BaseLight(
                    modifier = Modifier.fillMaxSize(),
                    text = "mobile.paymentAccounts.wise.picker.noResults".i18n(),
                    color = BisqTheme.colors.mid_grey20,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
                ) {
                    items(currencies, key = { it.code }) { currency ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BisqCheckbox(
                                checked = selectedCurrencyCodes.contains(currency.code),
                                label = currency.displayName,
                                onCheckedChange = { onToggle(currency.code) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun filterCurrencies(
    currencies: List<WiseCurrencyItem>,
    query: String,
): List<WiseCurrencyItem> {
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
private fun WiseCurrencyPickerBottomSheetPreview() {
    BisqTheme.Preview {
        WiseCurrencyPickerBottomSheet(
            selectedCurrencyCodes = setOf("USD", "EUR"),
            currencies =
                listOf(
                    WiseCurrencyItem("USD", "USD (US Dollar)"),
                    WiseCurrencyItem("EUR", "EUR (Euro)"),
                    WiseCurrencyItem("GBP", "GBP (British Pound)"),
                    WiseCurrencyItem("CAD", "CAD (Canadian Dollar)"),
                ),
            searchQuery = "",
            selectedCount = 2,
            totalCount = 4,
            onSearchChange = {},
            onToggle = {},
            onSelectAll = {},
            onClearAll = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun WiseFormContentPreview_DefaultPreview() {
    BisqTheme.Preview {
        WiseFormContent(
            uiState =
                WiseFormUiState(
                    holderNameEntry = DataEntry(value = "Satoshi Nakamoto"),
                    emailEntry = DataEntry(value = "satoshi@example.com"),
                    availableCurrencies =
                        listOf(
                            WiseCurrencyItem("USD", "USD (US Dollar)"),
                            WiseCurrencyItem("EUR", "EUR (Euro)"),
                            WiseCurrencyItem("GBP", "GBP (British Pound)"),
                        ),
                    selectedCurrencyCodes = setOf("USD", "EUR"),
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun WiseFormContentPreview_ErrorPreview() {
    BisqTheme.Preview {
        WiseFormContent(
            uiState =
                WiseFormUiState(
                    holderNameEntry = DataEntry(value = "a", errorMessage = "validation.tooShortOrTooLong".i18n(3, 100)),
                    emailEntry = DataEntry(value = "bad-email", errorMessage = "validation.invalidEmail".i18n()),
                    availableCurrencies =
                        listOf(
                            WiseCurrencyItem("USD", "USD (US Dollar)"),
                            WiseCurrencyItem("EUR", "EUR (Euro)"),
                        ),
                    selectedCurrencyCodes = emptySet(),
                    currencyErrorMessage = "mobile.paymentAccounts.wise.currencies.error".i18n(),
                ),
            onAction = {},
        )
    }
}
