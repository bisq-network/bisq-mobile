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
import androidx.compose.runtime.remember
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

data class CountryPickerItem(
    val code: String,
    val displayName: String,
)

@Composable
fun CountrySummaryRow(
    selectedCount: Int,
    totalCount: Int,
    isError: Boolean,
    onClick: () -> Unit,
) {
    val summaryText =
        if (selectedCount == totalCount && totalCount > 0) {
            "mobile.paymentAccounts.countryPicker.allSelected".i18n(totalCount)
        } else {
            "mobile.paymentAccounts.countryPicker.summary".i18n(selectedCount, totalCount)
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
                text = "mobile.paymentAccounts.picker.editLabel".i18n(),
                color = if (isError) BisqTheme.colors.danger else BisqTheme.colors.mid_grey20,
            )
        }
    }
}

@Composable
fun CountryPickerBottomSheet(
    selectedCountryCodes: Set<String>,
    countries: List<CountryPickerItem>,
    searchQuery: String,
    selectedCount: Int,
    totalCount: Int,
    onSearchChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val filteredCountries =
        remember(countries, searchQuery) {
            filterCountries(
                countries = countries,
                query = searchQuery,
            )
        }

    BisqBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H5Regular("paymentAccounts.createAccount.accountData.sepa.acceptCountries".i18n())
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
                            text = "mobile.paymentAccounts.picker.selectAll".i18n(),
                            color = BisqTheme.colors.primary,
                            modifier = Modifier.padding(4.dp),
                        )
                    }

                    Surface(onClick = onClearAll, color = BisqTheme.colors.transparent) {
                        BisqText.SmallRegular(
                            text = "mobile.paymentAccounts.picker.clearAll".i18n(),
                            color = BisqTheme.colors.mid_grey20,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }

                BisqText.SmallLight(
                    text = "mobile.paymentAccounts.countryPicker.summary".i18n(selectedCount, totalCount),
                    color = BisqTheme.colors.mid_grey20,
                )
            }

            BisqGap.V1()

            BisqSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "mobile.paymentAccounts.countryPicker.searchHint".i18n(),
            )

            BisqGap.V1()

            if (filteredCountries.isEmpty()) {
                BisqText.BaseLight(
                    modifier = Modifier.fillMaxSize(),
                    text = "mobile.paymentAccounts.countryPicker.noResults".i18n(),
                    color = BisqTheme.colors.mid_grey20,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
                ) {
                    items(filteredCountries, key = { it.code }) { country ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BisqCheckbox(
                                checked = selectedCountryCodes.contains(country.code),
                                label = country.displayName,
                                onCheckedChange = { onToggle(country.code) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun filterCountries(
    countries: List<CountryPickerItem>,
    query: String,
): List<CountryPickerItem> {
    if (query.isBlank()) {
        return countries
    }

    return countries.filter { country ->
        country.code.contains(query, ignoreCase = true) ||
            country.displayName.contains(query, ignoreCase = true)
    }
}

@Preview
@Composable
private fun CountrySummaryRowPreview() {
    BisqTheme.Preview {
        CountrySummaryRow(
            selectedCount = 2,
            totalCount = 4,
            isError = false,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun CountryPickerBottomSheetPreview() {
    BisqTheme.Preview {
        CountryPickerBottomSheet(
            selectedCountryCodes = setOf("DE", "FR"),
            countries =
                listOf(
                    CountryPickerItem("DE", "Germany"),
                    CountryPickerItem("FR", "France"),
                    CountryPickerItem("ES", "Spain"),
                    CountryPickerItem("NL", "Netherlands"),
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
