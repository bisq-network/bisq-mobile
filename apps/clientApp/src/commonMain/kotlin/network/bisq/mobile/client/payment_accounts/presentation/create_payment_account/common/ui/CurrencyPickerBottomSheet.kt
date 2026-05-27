package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

data class CurrencyPickerItem(
    val code: String,
    val displayName: String,
)

@Composable
fun CurrencySummaryRow(
    selectedCount: Int,
    totalCount: Int,
    isError: Boolean,
    onClick: () -> Unit,
) {
    val summaryText =
        if (selectedCount == totalCount && totalCount > 0) {
            "mobile.paymentAccounts.currencyPicker.allSelected".i18n(totalCount)
        } else {
            "mobile.paymentAccounts.currencyPicker.summary".i18n(selectedCount, totalCount)
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
                text = "mobile.paymentAccounts.currencyPicker.editLabel".i18n(),
                color = if (isError) BisqTheme.colors.danger else BisqTheme.colors.mid_grey20,
            )
        }
    }
}

@Composable
fun CurrencyPickerBottomSheet(
    selectedCurrencyCodes: Set<String>,
    currencies: List<CurrencyPickerItem>,
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
            BisqText.H5Regular("mobile.paymentAccounts.currencyPicker.title".i18n())
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
                            text = "mobile.paymentAccounts.currencyPicker.selectAll".i18n(),
                            color = BisqTheme.colors.primary,
                            modifier = Modifier.padding(4.dp),
                        )
                    }

                    Surface(onClick = onClearAll, color = BisqTheme.colors.transparent) {
                        BisqText.SmallRegular(
                            text = "mobile.paymentAccounts.currencyPicker.clearAll".i18n(),
                            color = BisqTheme.colors.mid_grey20,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }

                BisqText.SmallLight(
                    text = "mobile.paymentAccounts.currencyPicker.summary".i18n(selectedCount, totalCount),
                    color = BisqTheme.colors.mid_grey20,
                )
            }

            BisqGap.V1()

            BisqSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "mobile.paymentAccounts.currencyPicker.searchHint".i18n(),
            )

            BisqGap.V1()

            if (currencies.isEmpty()) {
                BisqText.BaseLight(
                    modifier = Modifier.fillMaxSize(),
                    text = "mobile.paymentAccounts.currencyPicker.noResults".i18n(),
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

@Preview
@Composable
private fun CurrencySummaryRowPreview() {
    BisqTheme.Preview {
        CurrencySummaryRow(
            selectedCount = 2,
            totalCount = 4,
            isError = false,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun CurrencySummaryRowErrorPreview() {
    BisqTheme.Preview {
        CurrencySummaryRow(
            selectedCount = 0,
            totalCount = 4,
            isError = true,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun CurrencyPickerBottomSheetPreview() {
    BisqTheme.Preview {
        CurrencyPickerBottomSheet(
            selectedCurrencyCodes = setOf("USD", "EUR"),
            currencies =
                listOf(
                    CurrencyPickerItem("USD", "USD (US Dollar)"),
                    CurrencyPickerItem("EUR", "EUR (Euro)"),
                    CurrencyPickerItem("GBP", "GBP (British Pound)"),
                    CurrencyPickerItem("CAD", "CAD (Canadian Dollar)"),
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
