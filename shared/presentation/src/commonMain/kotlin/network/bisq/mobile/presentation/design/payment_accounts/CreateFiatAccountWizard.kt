/**
 * CreateFiatAccountWizard.kt — Design PoC (Issue #991)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 * This feature is INTENTIONALLY HIDDEN until the MuSig protocol is finalized in Bisq2.
 * Do not expose this screen via any navigation route until MuSig is production-ready.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Mobile-adapted wizard for creating a fiat payment account. Covers the full creation
 * flow from payment method selection through account data entry to a review summary.
 *
 * The wizard is composed of three steps rendered in a MultiScreenWizardScaffold:
 *   Step 1 — Select Payment Method
 *   Step 2 — Enter Account Data
 *   Step 3 — Review & Confirm
 *
 * ======================================================================================
 * DESKTOP ADAPTATION
 * ======================================================================================
 * Desktop has a 4-step wizard:
 *   1. Select payment method (table: name | currencies | countries | chargeback risk)
 *   2. Enter method-specific account data
 *   3. Payment options (optional: which currencies/countries to offer)
 *   4. Summary & confirm
 *
 * Mobile merges steps 3 and 4 by embedding the optional payment options inline on the
 * account data step. Rationale:
 *   - Most users on mobile don't customize per-trade currency/country filters
 *   - Four full-screen wizard steps feels heavy for a settings task (compare: iOS
 *     Contacts app creates a contact in one scrollable form)
 *   - The merged step 2 shows fields progressively (advanced options collapsed by default)
 *
 * Step 1 adaptation: desktop shows a sortable table. Mobile replaces this with:
 *   - A BisqSearchField at the top for text search
 *   - Filter chips below the search field for risk level (All / Very Low / Low / Moderate)
 *   - Vertical list of PaymentTypeCard rows (existing component, consistent with
 *     the create-offer wizard's payment method selection step)
 *
 * Step 2 adaptation: desktop renders a method-specific form via polymorphism (each
 * payment method has its own Form class). Mobile POC shows two representative forms:
 *   - CUSTOM: matches current behavior (name + free-text accountData)
 *   - Structured (e.g., SEPA): account holder name + account identifier field
 *     with method-specific label and keyboard type (IBAN → text, email → email, etc.)
 *
 * ======================================================================================
 * PAYMENT METHOD CATALOGUE (Step 1 data)
 * ======================================================================================
 * The POC hardcodes a representative subset of FiatPaymentRailEnum values:
 *   SEPA, SEPA_INSTANT, ZELLE, REVOLUT, WISE, NATIONAL_BANK, ACH_TRANSFER,
 *   PIX, FASTER_PAYMENTS, STRIKE, CASH_APP, INTERAC_E_TRANSFER, BIZUM, CUSTOM
 *
 * Each entry carries: methodId, displayName, currencies, chargebackRisk.
 * In production, this list is driven by the backend's supported payment rails.
 *
 * ======================================================================================
 * CHARGEBACK RISK IN STEP 1
 * ======================================================================================
 * Chargeback risk is displayed as a color-coded badge on each row in Step 1. This
 * surfaces the risk BEFORE the user commits to a payment method, matching the intent
 * of the desktop table but surfacing it more prominently.
 *
 * Decision: show risk in the selection list, NOT hidden in a detail view. Users must
 * see risk level when choosing a method, not after entering account data.
 *
 * ======================================================================================
 * I18N CONSIDERATIONS
 * ======================================================================================
 * - Step headings ("Select Payment Method", "Account Details", "Review") are short.
 * - Payment method names are rendered via i18NPaymentMethod utility in production.
 *   POC hardcodes English; real implementation uses the existing payment method i18n
 *   infrastructure from the create-offer wizard.
 * - Form field labels ("Account Name", "Account Holder", "IBAN / Account Number") will
 *   need method-specific i18n keys. Consider a namespace:
 *     mobile.paymentAccounts.form.{methodId}.holderLabel
 *     mobile.paymentAccounts.form.{methodId}.identifierLabel
 *     mobile.paymentAccounts.form.{methodId}.identifierHint
 * - Risk filter chip labels need 3 i18n keys (see PaymentAccountCard.kt)
 * - Search field placeholder: re-use existing "action.search" key
 * - "Back" and "Next" buttons inherit from MultiScreenWizardScaffold defaults
 *   ("action.back" / "action.next") — already localized in 14 languages.
 */
package network.bisq.mobile.presentation.design.payment_accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethodIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountDetailFieldRow

// -------------------------------------------------------------------------------------
// Simulated data for preview purposes
// -------------------------------------------------------------------------------------

/**
 * Risk-level filter options for Step 1 search/filter row.
 * "ALL" means no filter applied.
 */
enum class RiskFilter {
    ALL,
    VERY_LOW,
    LOW,
    MODERATE,
}

/**
 * Simulated country availability for a payment method.
 * Maps to Bisq2's FiatPaymentRail.supportedCountries:
 *   - Single country (e.g., ACH_TRANSFER → US)
 *   - Regional list (e.g., SEPA → 37 SEPA zone countries)
 *   - All countries (e.g., National Bank, Cash by Mail, F2F)
 *
 * Production implementation derives this from FiatPaymentRail + CountryRepository.
 */
sealed class SimulatedCountryAvailability {
    /** Available in all countries worldwide */
    data object Worldwide : SimulatedCountryAvailability()

    /** Available in a specific set of countries (by ISO 3166 2-letter codes) */
    data class Countries(
        val codes: List<String>,
    ) : SimulatedCountryAvailability()
}

/**
 * Simulated payment method entry for the Step 1 selection list.
 * Production implementation sources this from FiatPaymentRailEnum + backend API.
 */
data class SimulatedPaymentMethod(
    val methodId: String,
    val displayName: String,
    val currencies: List<String>,
    val chargebackRisk: SimulatedChargebackRisk,
    val countryAvailability: SimulatedCountryAvailability = SimulatedCountryAvailability.Worldwide,
)

/**
 * Simulated form field descriptor for Step 2.
 * In production each payment method has a specific form schema.
 */
data class SimulatedFormField(
    val fieldId: String,
    val label: String,
    val placeholder: String,
    val isMultiLine: Boolean = false,
)

// Representative SEPA zone countries (37 total, matching Bisq2's FiatPaymentRailUtil)
internal val sepaCountries =
    listOf(
        "AT",
        "BE",
        "BG",
        "CY",
        "CZ",
        "DE",
        "DK",
        "EE",
        "ES",
        "FI",
        "FR",
        "GB",
        "GI",
        "GR",
        "HR",
        "HU",
        "IE",
        "IS",
        "IT",
        "JE",
        "LI",
        "LT",
        "LU",
        "LV",
        "MC",
        "MT",
        "NL",
        "NO",
        "PL",
        "PT",
        "RO",
        "SE",
        "SI",
        "SK",
        "SM",
        "VA",
        "CH",
    )

// Representative Revolut-supported countries (subset — full list is ~30 countries)
internal val revolutCountries =
    listOf(
        "AT",
        "AU",
        "BE",
        "BG",
        "CA",
        "CH",
        "CY",
        "CZ",
        "DE",
        "DK",
        "EE",
        "ES",
        "FI",
        "FR",
        "GB",
        "GR",
        "HR",
        "HU",
        "IE",
        "IS",
        "IT",
        "JP",
        "LI",
        "LT",
        "LU",
        "LV",
        "MT",
        "NL",
        "NO",
        "NZ",
        "PL",
        "PT",
        "RO",
        "SE",
        "SG",
        "SI",
        "SK",
        "US",
    )

// Sample data used across multiple preview variants
// Country mappings replicate Bisq2's FiatPaymentRail.supportedCountries
private val allPaymentMethods =
    listOf(
        SimulatedPaymentMethod("SEPA", "SEPA", listOf("EUR"), SimulatedChargebackRisk.VERY_LOW, SimulatedCountryAvailability.Countries(sepaCountries)),
        SimulatedPaymentMethod("SEPA_INSTANT", "SEPA Instant", listOf("EUR"), SimulatedChargebackRisk.VERY_LOW, SimulatedCountryAvailability.Countries(sepaCountries)),
        SimulatedPaymentMethod("FASTER_PAYMENTS", "Faster Payments", listOf("GBP"), SimulatedChargebackRisk.VERY_LOW, SimulatedCountryAvailability.Countries(listOf("GB"))),
        SimulatedPaymentMethod("PIX", "PIX", listOf("BRL"), SimulatedChargebackRisk.VERY_LOW, SimulatedCountryAvailability.Countries(listOf("BR"))),
        SimulatedPaymentMethod("BIZUM", "Bizum", listOf("EUR"), SimulatedChargebackRisk.VERY_LOW, SimulatedCountryAvailability.Countries(listOf("ES"))),
        SimulatedPaymentMethod("REVOLUT", "Revolut", listOf("EUR", "USD", "GBP"), SimulatedChargebackRisk.LOW, SimulatedCountryAvailability.Countries(revolutCountries)),
        SimulatedPaymentMethod("WISE", "Wise", listOf("EUR", "USD", "GBP"), SimulatedChargebackRisk.LOW, SimulatedCountryAvailability.Worldwide),
        SimulatedPaymentMethod("INTERAC_E_TRANSFER", "Interac e-Transfer", listOf("CAD"), SimulatedChargebackRisk.LOW, SimulatedCountryAvailability.Countries(listOf("CA"))),
        SimulatedPaymentMethod("STRIKE", "Strike", listOf("USD"), SimulatedChargebackRisk.LOW, SimulatedCountryAvailability.Countries(listOf("US"))),
        SimulatedPaymentMethod("ZELLE", "Zelle", listOf("USD"), SimulatedChargebackRisk.MODERATE, SimulatedCountryAvailability.Countries(listOf("US"))),
        SimulatedPaymentMethod("ACH_TRANSFER", "ACH Transfer", listOf("USD"), SimulatedChargebackRisk.MODERATE, SimulatedCountryAvailability.Countries(listOf("US"))),
        SimulatedPaymentMethod("CASH_APP", "Cash App", listOf("USD"), SimulatedChargebackRisk.MODERATE, SimulatedCountryAvailability.Countries(listOf("US"))),
        SimulatedPaymentMethod("NATIONAL_BANK", "National Bank Transfer", listOf("*"), SimulatedChargebackRisk.LOW, SimulatedCountryAvailability.Worldwide),
        SimulatedPaymentMethod("CUSTOM", "Custom", listOf("*"), SimulatedChargebackRisk.LOW, SimulatedCountryAvailability.Worldwide),
    )

private val sepaFormFields =
    listOf(
        SimulatedFormField("accountHolder", "Account Holder Name", "Full name as on bank account"),
        SimulatedFormField("iban", "IBAN", "e.g. DE89 3704 0044 0532 0130 00"),
        SimulatedFormField("bic", "BIC / SWIFT", "e.g. COBADEFFXXX"),
    )

private val customFormFields =
    listOf(
        SimulatedFormField("accountName", "Account Name", "e.g. My PayPal"),
        SimulatedFormField(
            "accountData",
            "Account Data",
            "Enter your payment details here…",
            isMultiLine = true,
        ),
    )

// -------------------------------------------------------------------------------------
// Step 1: Select Payment Method
// -------------------------------------------------------------------------------------

/**
 * Step 1 content — payment method selection list with search and risk filter.
 *
 * Layout:
 *   BisqSearchField (full width)
 *   FlowRow of risk filter chips: [All] [Very Low] [Low] [Moderate]
 *   Vertical list of PaymentTypeCard rows (one per matching method)
 *
 * The filter chips use RiskFilterChip with outline/active state toggling.
 * The "All" chip acts as a reset — selecting any risk-level chip deactivates "All",
 * and selecting "All" clears the risk-level selection.
 *
 * PaymentMethodSelectionRow extends the PaymentTypeCard visual pattern by adding a
 * trailing chargeback-risk badge. This avoids modifying the shared PaymentTypeCard
 * component while providing risk context inline.
 *
 * @param methods Full list of available payment methods to display
 * @param searchQuery Current search query string
 * @param activeRiskFilter Currently active risk filter
 * @param selectedMethodId Currently selected method ID (null if none selected)
 * @param onSearchChange Called when the user types in the search field
 * @param onRiskFilterChange Called when the user selects a risk filter chip
 * @param onMethodSelect Called with the methodId when user selects a method row
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateFiatAccount_Step1_SelectMethod(
    methods: List<SimulatedPaymentMethod>,
    searchQuery: String,
    activeRiskFilter: RiskFilter,
    selectedMethodId: String?,
    onSearchChange: (String) -> Unit,
    onRiskFilterChange: (RiskFilter) -> Unit,
    onMethodSelect: (String) -> Unit,
) {
    val filteredMethods =
        methods
            .filter { method ->
                (searchQuery.isBlank() || method.displayName.contains(searchQuery, ignoreCase = true)) &&
                    (
                        activeRiskFilter == RiskFilter.ALL || method.chargebackRisk ==
                            when (activeRiskFilter) {
                                RiskFilter.VERY_LOW -> SimulatedChargebackRisk.VERY_LOW
                                RiskFilter.LOW -> SimulatedChargebackRisk.LOW
                                RiskFilter.MODERATE -> SimulatedChargebackRisk.MODERATE
                                RiskFilter.ALL -> null
                            }
                    )
            }

    // Bottom sheet state for showing full country list
    var countrySheetMethod by remember { mutableStateOf<SimulatedPaymentMethod?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H3Light("Select a payment method")
        BisqText.SmallLight(
            "Choose how your trading partner will send you fiat. " +
                "Methods with lower chargeback risk are preferred.",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.VHalf()

        // Search field
        BisqSearchField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = "Search payment methods",
        )

        // Risk filter chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            RiskFilterChip(
                label = "All",
                isSelected = activeRiskFilter == RiskFilter.ALL,
                onClick = { onRiskFilterChange(RiskFilter.ALL) },
            )
            RiskFilterChip(
                label = "Very Low Risk",
                isSelected = activeRiskFilter == RiskFilter.VERY_LOW,
                color = BisqTheme.colors.primary,
                onClick = { onRiskFilterChange(RiskFilter.VERY_LOW) },
            )
            RiskFilterChip(
                label = "Low Risk",
                isSelected = activeRiskFilter == RiskFilter.LOW,
                color = BisqTheme.colors.warning,
                onClick = { onRiskFilterChange(RiskFilter.LOW) },
            )
            RiskFilterChip(
                label = "Moderate Risk",
                isSelected = activeRiskFilter == RiskFilter.MODERATE,
                color = BisqTheme.colors.danger,
                onClick = { onRiskFilterChange(RiskFilter.MODERATE) },
            )
        }

        BisqGap.VHalf()

        // Payment method list
        if (filteredMethods.isEmpty()) {
            BisqText.BaseLight(
                "No payment methods match your search.",
                color = BisqTheme.colors.mid_grey20,
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                filteredMethods.forEach { method ->
                    PaymentMethodSelectionRow(
                        method = method,
                        isSelected = method.methodId == selectedMethodId,
                        onSelect = { onMethodSelect(method.methodId) },
                        onShowCountries = { countrySheetMethod = method },
                    )
                }
            }
        }
    }

    // Country list bottom sheet — shown when tapping "N countries" on a regional method
    countrySheetMethod?.let { method ->
        val countries = (method.countryAvailability as? SimulatedCountryAvailability.Countries)?.codes ?: return@let
        CountryListBottomSheet(
            methodName = method.displayName,
            countryCodes = countries,
            onDismiss = { countrySheetMethod = null },
        )
    }
}

/**
 * A single row in the payment method selection list.
 *
 * Extends PaymentTypeCard's visual pattern by adding:
 *   - A trailing chargeback risk badge for immediate risk visibility
 *   - A country availability subtitle showing where the method is available
 *
 * Country subtitle rendering:
 *   - Single country: full country name (e.g., "United States")
 *   - Regional list (≤5 countries): all codes listed (e.g., "DE, FR, IT, ES, NL")
 *   - Regional list (>5 countries): first 3 codes + tappable "+N more" link
 *     that opens a bottom sheet with the full country list
 *   - Worldwide: "Available worldwide"
 *
 * The row uses dark_grey50 background (same as PaymentTypeCard) and primaryDim
 * when selected for consistency with the create-offer wizard.
 */
@Composable
private fun PaymentMethodSelectionRow(
    method: SimulatedPaymentMethod,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onShowCountries: () -> Unit,
) {
    val riskColor =
        when (method.chargebackRisk) {
            SimulatedChargebackRisk.VERY_LOW -> BisqTheme.colors.primary
            SimulatedChargebackRisk.LOW -> BisqTheme.colors.warning
            SimulatedChargebackRisk.MODERATE -> BisqTheme.colors.danger
        }

    // Outer clickable card — wraps PaymentTypeCard pattern but adds trailing badge
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = if (isSelected) BisqTheme.colors.primaryDim else BisqTheme.colors.dark_grey50,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding2X,
                        vertical = BisqUIConstants.ScreenPadding,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            // Method icon (20dp — consistent with create-offer payment step)
            PaymentMethodIcon(
                methodId = method.methodId,
                isPaymentMethod = true,
                size = BisqUIConstants.ScreenPadding2X,
                contentDescription = method.displayName,
            )

            // Method name + currencies + country availability
            Column(modifier = Modifier.weight(1f)) {
                BisqText.BaseRegular(method.displayName)
                if (method.currencies.isNotEmpty() && method.currencies != listOf("*")) {
                    BisqText.SmallLight(
                        method.currencies.joinToString(", "),
                        color = BisqTheme.colors.mid_grey20,
                    )
                }
                // Country availability subtitle
                CountryAvailabilitySubtitle(
                    availability = method.countryAvailability,
                    onShowFullList = onShowCountries,
                )
            }

            // Chargeback risk badge (trailing)
            Surface(
                shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                color = riskColor.copy(alpha = 0.15f),
            ) {
                BisqText.SmallRegular(
                    text =
                        when (method.chargebackRisk) {
                            SimulatedChargebackRisk.VERY_LOW -> "Very Low"
                            SimulatedChargebackRisk.LOW -> "Low"
                            SimulatedChargebackRisk.MODERATE -> "Moderate"
                        },
                    color = riskColor,
                    modifier =
                        Modifier.padding(
                            horizontal = BisqUIConstants.ScreenPaddingHalf,
                            vertical = BisqUIConstants.ScreenPaddingQuarter,
                        ),
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Country availability components
// -------------------------------------------------------------------------------------

/**
 * Compact country availability subtitle for payment method rows and account cards.
 * Shared across CreateFiatAccountWizard and PaymentAccountCard.
 *
 * Rendering strategy:
 *   - Worldwide → "Available worldwide" in subdued grey
 *   - Single country → full country name via ISO code lookup
 *   - ≤5 countries → comma-separated codes
 *   - >5 countries → first 3 codes + tappable "+N more" link (underlined, primary color)
 *
 * The "+N more" link opens a bottom sheet with the full sorted country list.
 * This keeps the row compact while still making the full list accessible.
 *
 * Production implementation should use CountryRepository for code → name mapping.
 * POC uses a hardcoded subset of common country names.
 */
@Composable
internal fun CountryAvailabilitySubtitle(
    availability: SimulatedCountryAvailability,
    onShowFullList: () -> Unit,
) {
    when (availability) {
        is SimulatedCountryAvailability.Worldwide -> {
            BisqText.SmallLight(
                "Available worldwide",
                color = BisqTheme.colors.mid_grey30,
            )
        }

        is SimulatedCountryAvailability.Countries -> {
            val codes = availability.codes
            when {
                codes.size == 1 -> {
                    BisqText.SmallLight(
                        countryName(codes.first()),
                        color = BisqTheme.colors.mid_grey30,
                    )
                }

                codes.size <= 5 -> {
                    BisqText.SmallLight(
                        codes.sorted().joinToString(", "),
                        color = BisqTheme.colors.mid_grey30,
                    )
                }

                else -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        BisqText.SmallLight(
                            codes.sorted().take(3).joinToString(", ") + " ",
                            color = BisqTheme.colors.mid_grey30,
                        )
                        Text(
                            "+${codes.size - 3} more",
                            style =
                                BisqTheme.typography.smallRegular.copy(
                                    color = BisqTheme.colors.primary,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            modifier = Modifier.clickable { onShowFullList() },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet showing the full list of supported countries for a payment method.
 *
 * Displays the method name as a header, followed by a scrollable list of country
 * entries (flag placeholder + code + name). Uses LazyColumn for efficient rendering
 * of long lists (e.g., SEPA's 37 countries, Revolut's 38 countries).
 *
 * Production implementation should:
 *   - Source country names from CountryRepository.getNameByCode()
 *   - Optionally show flag icons via DynamicImage with country flag assets
 */
@Composable
internal fun CountryListBottomSheet(
    methodName: String,
    countryCodes: List<String>,
    onDismiss: () -> Unit,
) {
    BisqBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H5Regular("$methodName — Supported Countries")
            BisqGap.VHalf()
            BisqText.SmallLight(
                "${countryCodes.size} countries",
                color = BisqTheme.colors.mid_grey20,
            )
            BisqGap.V1()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                items(countryCodes.sorted()) { code ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = BisqUIConstants.ScreenPaddingQuarter),
                        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Country code badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BisqTheme.colors.dark_grey40,
                        ) {
                            BisqText.SmallRegular(
                                code,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        // Country name
                        BisqText.BaseLight(
                            countryName(code),
                            color = BisqTheme.colors.white,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simulated country code → name lookup for POC previews.
 * Production implementation uses CountryRepository.getNameByCode().
 */
internal fun countryName(code: String): String = COUNTRY_NAMES[code] ?: code

internal val COUNTRY_NAMES =
    mapOf(
        "AD" to "Andorra",
        "AR" to "Argentina",
        "AT" to "Austria",
        "AU" to "Australia",
        "BE" to "Belgium",
        "BG" to "Bulgaria",
        "BR" to "Brazil",
        "CA" to "Canada",
        "CH" to "Switzerland",
        "CN" to "China",
        "CY" to "Cyprus",
        "CZ" to "Czech Republic",
        "DE" to "Germany",
        "DK" to "Denmark",
        "EE" to "Estonia",
        "ES" to "Spain",
        "FI" to "Finland",
        "FR" to "France",
        "GB" to "United Kingdom",
        "GI" to "Gibraltar",
        "GR" to "Greece",
        "HR" to "Croatia",
        "HU" to "Hungary",
        "IE" to "Ireland",
        "IN" to "India",
        "IS" to "Iceland",
        "IT" to "Italy",
        "JE" to "Jersey",
        "JP" to "Japan",
        "LI" to "Liechtenstein",
        "LT" to "Lithuania",
        "LU" to "Luxembourg",
        "LV" to "Latvia",
        "MC" to "Monaco",
        "MT" to "Malta",
        "NL" to "Netherlands",
        "NO" to "Norway",
        "NZ" to "New Zealand",
        "PL" to "Poland",
        "PT" to "Portugal",
        "RO" to "Romania",
        "RU" to "Russia",
        "SE" to "Sweden",
        "SG" to "Singapore",
        "SI" to "Slovenia",
        "SK" to "Slovakia",
        "SM" to "San Marino",
        "TH" to "Thailand",
        "US" to "United States",
        "VA" to "Vatican City",
    )

/**
 * A filter chip for risk-level filtering in Step 1.
 *
 * Selected state uses a filled background (color at 20% alpha) + colored text.
 * Unselected state uses dark_grey40 background + mid_grey20 text.
 * Color parameter defaults to white for the "All" chip.
 */
@Composable
private fun RiskFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color = BisqTheme.colors.white,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = if (isSelected) color.copy(alpha = 0.15f) else BisqTheme.colors.dark_grey40,
    ) {
        BisqText.SmallRegular(
            label,
            color = if (isSelected) color else BisqTheme.colors.mid_grey20,
            modifier =
                Modifier.padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
        )
    }
}

// -------------------------------------------------------------------------------------
// Step 2: Account Data Form
// -------------------------------------------------------------------------------------

/**
 * Step 2 content — method-specific account data entry form.
 *
 * The form is parameterized by [formFields] — a list of [SimulatedFormField] descriptors.
 * Each field descriptor drives one BisqTextFieldV0 input with appropriate label,
 * placeholder, and multi-line configuration.
 *
 * This approach allows the same composable to render both CUSTOM (2 fields) and
 * structured methods like SEPA (3 fields: holder name, IBAN, BIC) without branching.
 * In production, the field descriptors are generated from the selected payment method's
 * form schema (adapting Bisq2's PaymentAccountFormFactory pattern).
 *
 * Above the form, a summary row shows the selected method's icon and name. This gives
 * the user a clear "you are creating a SEPA account" confirmation at step 2 entry,
 * reducing confusion if they navigated back and changed method.
 *
 * @param selectedMethodDisplayName Display name of the selected payment method
 * @param selectedMethodId ID of the selected payment method (for icon)
 * @param formFields Ordered list of form fields to render
 * @param fieldValues Current value for each field (keyed by fieldId)
 * @param onFieldChange Called with (fieldId, newValue) when a field changes
 */
@Composable
fun CreateFiatAccount_Step2_AccountData(
    selectedMethodDisplayName: String,
    selectedMethodId: String,
    formFields: List<SimulatedFormField>,
    fieldValues: Map<String, String>,
    onFieldChange: (String, String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H3Light("Account details")
        BisqText.SmallLight(
            "Enter the details for your $selectedMethodDisplayName account. " +
                "These will be shared with your trading partner when you confirm a trade.",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.VHalf()

        // Selected method recap — confirmation the user is on the right method
        BisqCard(
            backgroundColor = BisqTheme.colors.dark_grey50,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                PaymentMethodIcon(
                    methodId = selectedMethodId,
                    isPaymentMethod = true,
                    size = BisqUIConstants.ScreenPadding2X,
                    contentDescription = selectedMethodDisplayName,
                )
                BisqText.BaseRegular(selectedMethodDisplayName)
            }
        }

        BisqGap.VHalf()

        // Dynamic form fields
        formFields.forEach { field ->
            BisqTextFieldV0(
                value = fieldValues[field.fieldId] ?: "",
                onValueChange = { onFieldChange(field.fieldId, it) },
                label = field.label,
                placeholder = field.placeholder,
                minLines = if (field.isMultiLine) 4 else 1,
            )
            BisqGap.VHalf()
        }

        // Privacy note — always shown, regardless of method
        // Users should understand account data is stored locally only
        BisqText.SmallLight(
            "Your account details are stored locally on your device and encrypted at rest. " +
                "They are only shared with your trading partner after you initiate a trade.",
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

// -------------------------------------------------------------------------------------
// Step 3: Review & Confirm
// -------------------------------------------------------------------------------------

/**
 * Step 3 content — summary of all entered account data before saving.
 *
 * Presents a read-only BisqCard with all field values. The user can tap "Back" to
 * return to Step 2 and correct data, or "Confirm" (via the wizard's Next button) to save.
 *
 * The summary also re-shows the chargeback risk badge as a final reminder.
 * This is the last opportunity to educate the user before the account is persisted.
 *
 * Desktop analogy: Bisq2's "Summary" step in the payment account creation wizard.
 *
 * @param selectedMethod The payment method chosen in Step 1
 * @param formFields Field descriptors (for labels)
 * @param fieldValues Entered values keyed by fieldId
 */
@Composable
fun CreateFiatAccount_Step3_Review(
    selectedMethod: SimulatedPaymentMethod,
    formFields: List<SimulatedFormField>,
    fieldValues: Map<String, String>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H3Light("Review account")
        BisqText.SmallLight(
            "Confirm your account details before saving.",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.VHalf()

        BisqCard {
            // Method header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PaymentMethodIcon(
                    methodId = selectedMethod.methodId,
                    isPaymentMethod = true,
                    size = BisqUIConstants.ScreenPadding2X,
                    contentDescription = selectedMethod.displayName,
                )
                Column(modifier = Modifier.weight(1f)) {
                    BisqText.BaseRegular(selectedMethod.displayName)
                    if (selectedMethod.currencies.isNotEmpty() && selectedMethod.currencies != listOf("*")) {
                        BisqText.SmallLight(
                            selectedMethod.currencies.joinToString(", "),
                            color = BisqTheme.colors.mid_grey20,
                        )
                    }
                }
            }

            BisqGap.V1()

            // Country availability in review
            AccountDetailFieldRow(
                label = "Available in",
                value =
                    when (val avail = selectedMethod.countryAvailability) {
                        is SimulatedCountryAvailability.Worldwide -> "All countries"
                        is SimulatedCountryAvailability.Countries ->
                            when {
                                avail.codes.size == 1 -> countryName(avail.codes.first())
                                avail.codes.size <= 5 -> avail.codes.sorted().joinToString(", ") { countryName(it) }
                                else -> "${avail.codes.size} countries"
                            }
                    },
            )
            BisqGap.VHalf()

            // Field summary rows
            formFields.forEach { field ->
                AccountDetailFieldRow(
                    label = field.label,
                    value = fieldValues[field.fieldId] ?: "—",
                )
                BisqGap.VHalf()
            }

            BisqGap.VHalf()

            // Chargeback risk — final reminder
            ChargebackRiskBadge(risk = selectedMethod.chargebackRisk)
        }

        BisqGap.V1()

        BisqText.SmallLight(
            "Tapping Confirm will save this account to your device. " +
                "You can edit or delete it from the Payment Accounts screen at any time.",
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

// ChargebackRiskBadge is defined in PaymentAccountCard.kt and used here via internal visibility.

// -------------------------------------------------------------------------------------
// Full wizard composables (for @Preview)
// -------------------------------------------------------------------------------------

/**
 * Full wizard shell for Step 1 — used in previews and production entry.
 * State is intentionally local (remember) since this is a stateless POC.
 * Production implementation hoists state to a presenter.
 */
@Composable
private fun CreateFiatAccountWizard_Step1Preview(
    methods: List<SimulatedPaymentMethod> = allPaymentMethods,
    initialSearch: String = "",
    initialFilter: RiskFilter = RiskFilter.ALL,
    initialSelectedMethodId: String? = null,
) {
    var searchQuery by remember { mutableStateOf(initialSearch) }
    var activeFilter by remember { mutableStateOf(initialFilter) }
    var selectedMethodId by remember { mutableStateOf(initialSelectedMethodId) }

    // Clear selection when filter/search hides the selected method
    val visibleMethodIds =
        methods
            .filter { method ->
                (searchQuery.isBlank() || method.displayName.contains(searchQuery, ignoreCase = true)) &&
                    (
                        activeFilter == RiskFilter.ALL || method.chargebackRisk ==
                            when (activeFilter) {
                                RiskFilter.VERY_LOW -> SimulatedChargebackRisk.VERY_LOW
                                RiskFilter.LOW -> SimulatedChargebackRisk.LOW
                                RiskFilter.MODERATE -> SimulatedChargebackRisk.MODERATE
                                RiskFilter.ALL -> null
                            }
                    )
            }.map { it.methodId }
            .toSet()

    if (selectedMethodId != null && selectedMethodId !in visibleMethodIds) {
        selectedMethodId = null
    }

    MultiScreenWizardScaffold(
        title = "Add Fiat Account",
        stepIndex = 1,
        stepsLength = 3,
        nextButtonText = "Next",
        nextDisabled = selectedMethodId == null,
        nextOnClick = { /* preview no-op */ },
        prevDisabled = true,
        prevOnClick = null,
        closeAction = true,
    ) {
        CreateFiatAccount_Step1_SelectMethod(
            methods = methods,
            searchQuery = searchQuery,
            activeRiskFilter = activeFilter,
            selectedMethodId = selectedMethodId,
            onSearchChange = { searchQuery = it },
            onRiskFilterChange = { activeFilter = it },
            onMethodSelect = { selectedMethodId = it },
        )
    }
}

/**
 * Full wizard shell for Step 2 — custom method form.
 */
@Composable
private fun CreateFiatAccountWizard_Step2CustomPreview() {
    var fieldValues by remember {
        mutableStateOf(mapOf("accountName" to "", "accountData" to ""))
    }

    MultiScreenWizardScaffold(
        title = "Add Fiat Account",
        stepIndex = 2,
        stepsLength = 3,
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        CreateFiatAccount_Step2_AccountData(
            selectedMethodDisplayName = "Custom",
            selectedMethodId = "CUSTOM",
            formFields = customFormFields,
            fieldValues = fieldValues,
            onFieldChange = { id, value -> fieldValues = fieldValues + (id to value) },
        )
    }
}

/**
 * Full wizard shell for Step 2 — structured method form (SEPA example).
 */
@Composable
private fun CreateFiatAccountWizard_Step2StructuredPreview() {
    var fieldValues by remember {
        mutableStateOf(
            mapOf(
                "accountHolder" to "Satoshi Nakamoto",
                "iban" to "DE89 3704 0044 0532 0130 00",
                "bic" to "COBADEFFXXX",
            ),
        )
    }

    MultiScreenWizardScaffold(
        title = "Add Fiat Account",
        stepIndex = 2,
        stepsLength = 3,
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        CreateFiatAccount_Step2_AccountData(
            selectedMethodDisplayName = "SEPA",
            selectedMethodId = "SEPA",
            formFields = sepaFormFields,
            fieldValues = fieldValues,
            onFieldChange = { id, value -> fieldValues = fieldValues + (id to value) },
        )
    }
}

/**
 * Full wizard shell for Step 3 — review and confirm.
 */
@Composable
private fun CreateFiatAccountWizard_Step3Preview() {
    MultiScreenWizardScaffold(
        title = "Add Fiat Account",
        stepIndex = 3,
        stepsLength = 3,
        nextButtonText = "Confirm",
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        CreateFiatAccount_Step3_Review(
            selectedMethod =
                SimulatedPaymentMethod(
                    methodId = "SEPA",
                    displayName = "SEPA",
                    currencies = listOf("EUR"),
                    chargebackRisk = SimulatedChargebackRisk.VERY_LOW,
                    countryAvailability = SimulatedCountryAvailability.Countries(sepaCountries),
                ),
            formFields = sepaFormFields,
            fieldValues =
                mapOf(
                    "accountHolder" to "Satoshi Nakamoto",
                    "iban" to "DE89 3704 0044 0532 0130 00",
                    "bic" to "COBADEFFXXX",
                ),
        )
    }
}

// -------------------------------------------------------------------------------------
// @Preview functions
// -------------------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step1_DefaultPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step1Preview()
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step1_WithSearchPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step1Preview(
            initialSearch = "sepa",
            initialSelectedMethodId = "SEPA",
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step1_RiskFilterPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step1Preview(
            initialFilter = RiskFilter.VERY_LOW,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step2_CustomPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step2CustomPreview()
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step2_SepaStructuredPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step2StructuredPreview()
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step3_ReviewPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step3Preview()
    }
}
